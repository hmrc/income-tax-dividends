/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.SessionValues
import config.AppConfig
import helpers.{PlaySessionCookieBaker, WireMockHelper}
import models.User
import models.dividends.{DividendsCheckYourAnswersModel, StockDividendsCheckYourAnswersModel, StockDividendsPriorSubmission}
import models.mongo._
import models.priorDataModels.IncomeSourcesModel
import org.apache.pekko.actor.ActorSystem
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.HeaderNames
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{BodyWritable, WSClient, WSResponse}
import play.api.mvc.{AnyContent, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.OK
import play.api.{Application, Environment, Mode}
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import java.time.LocalDate
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

trait IntegrationTest extends AnyWordSpecLike with Matchers with GuiceOneServerPerSuite with WireMockHelper
  with BeforeAndAfterAll {

  val authorizationHeader: (String, String) = HeaderNames.AUTHORIZATION -> "mock-bearer-token"
  private val dateNow: LocalDate = LocalDate.now()
  private val taxYearCutoffDate: LocalDate = LocalDate.parse(s"${dateNow.getYear}-04-05")

  val taxYear: Int = if (dateNow.isAfter(taxYearCutoffDate)) LocalDate.now().getYear + 1 else LocalDate.now().getYear
  val taxYearEOY: Int = taxYear - 1
  val taxYearEndOfYearMinusOne: Int = taxYearEOY - 1

  val validTaxYearList: Seq[Int] = Seq(taxYearEOY - 1, taxYearEOY, taxYear)
  val singleValidTaxYear: Seq[Int] = Seq(taxYearEndOfYearMinusOne)

  val nino = "AA123456A"
  val mtditid = "1234567890"
  val sessionId = "sessionId-eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"

  val xSessionId: (String, String) = "X-Session-ID" -> sessionId
  val csrfContent: (String, String) = "Csrf-Token" -> "nocheck"

  val completeDividendsCYAModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(
    gateway = Some(true),
    ukDividends = Some(true),
    ukDividendsAmount = Some(50.00),
    otherUkDividends = Some(true),
    otherUkDividendsAmount = Some(50.00)
  )

  val completeStockDividendsCYAModel: StockDividendsCheckYourAnswersModel = StockDividendsCheckYourAnswersModel(
    gateway = Some(true),
    ukDividends = Some(true),
    ukDividendsAmount = Some(50.00),
    otherUkDividends = Some(true),
    otherUkDividendsAmount = Some(50.00),
    stockDividends = Some(true),
    stockDividendsAmount = Some(50.00),
    redeemableShares = Some(true),
    redeemableSharesAmount = Some(50.00),
    closeCompanyLoansWrittenOff = Some(true),
    closeCompanyLoansWrittenOffAmount = Some(50.00),
  )

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())
  lazy val welshMessages: Messages = messagesApi.preferred(Seq(Lang("cy")))

  implicit lazy val user: User[AnyContent] = new User[AnyContent](mtditid, None, nino, sessionId)(FakeRequest())

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid)

  implicit val actorSystem: ActorSystem = ActorSystem()

  val startUrl = s"http://localhost:$port/update-and-submit-income-tax-return/personal-income"
  val overviewUrl = s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view"

  implicit def wsClient: WSClient = app.injector.instanceOf[WSClient]

  val appUrl = s"http://localhost:$port/update-and-submit-income-tax-return/personal-income"

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  lazy val commonConfig: Map[String, Any] = Map(
    "auditing.enabled" -> false,
    "metrics.enabled" -> false,
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.income-tax-submission-frontend.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-submission.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.auth.host" -> wiremockHost,
    "microservice.services.auth.port" -> wiremockPort,
    "microservice.services.income-tax-dividends.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-interest.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-gift-aid.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.sign-in.url" -> s"/auth-login-stub/gg-sign-in"
  )

  def config(
    interestSavings: Boolean = false,
    stockDividends: Boolean = false
  ): Map[String, Any] = commonConfig ++ Map(
    "taxYearChangeResetsSession" -> false,
    "feature-switch.useEncryption" -> true,
    "defaultTaxYear" -> taxYear,
    "feature-switch.useEncryption" -> true,
    "feature-switch.journeys.stock-dividends" -> stockDividends,
    "feature-switch.journeys.interestSavings"-> interestSavings
  )

  def invalidEncryptionConfig: Map[String, Any] = commonConfig ++ Map(
    "taxYearChangeResetsSession" -> false,
    "feature-switch.useEncryption" -> true,
    "mongodb.encryption.key" -> "key",
    "defaultTaxYear" -> taxYear
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config())
    .build()

  lazy val appWithInvalidEncryptionKey: Application = GuiceApplicationBuilder()
    .configure(invalidEncryptionConfig)
    .build()

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

  lazy val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  def urlGet(url: String, welsh: Boolean = false, follow: Boolean = true, headers: Seq[(String, String)] = Seq())(implicit wsClient: WSClient): WSResponse = {

    val newHeaders = if (welsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") ++ headers else headers
    await(wsClient.url(url).withFollowRedirects(follow).withHttpHeaders(newHeaders: _*).get())
  }

  def urlPost[T](url: String,
                 body: T,
                 welsh: Boolean = false,
                 follow: Boolean = true,
                 headers: Seq[(String, String)] = Seq())
                (implicit wsClient: WSClient, bodyWritable: BodyWritable[T]): WSResponse = {

    val headersWithNoCheck = headers ++ Seq("Csrf-Token" -> "nocheck")
    val newHeaders = if (welsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") ++ headersWithNoCheck else headersWithNoCheck
    await(wsClient.url(url).withFollowRedirects(follow).withHttpHeaders(newHeaders: _*).post(body))
  }

  //noinspection ScalaStyle
  def playSessionCookie(agent: Boolean = false, extraData: Map[String, String] = Map.empty, validTaxYears:Seq[Int] = validTaxYearList, isEoy: Boolean = false): Seq[(String, String)] = {
    {
      if (agent) {
        Seq(HeaderNames.COOKIE -> PlaySessionCookieBaker.bakeSessionCookie(extraData ++ Map(
          SessionKeys.sessionId -> sessionId,
          SessionKeys.authToken -> "mock-bearer-token",
          SessionValues.TAX_YEAR -> (if(isEoy) taxYearEOY else taxYear).toString,
          SessionValues.VALID_TAX_YEARS -> validTaxYears.mkString(","),
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> mtditid))
        )
      } else {
        Seq(HeaderNames.COOKIE -> PlaySessionCookieBaker.bakeSessionCookie(extraData ++ Map(
          SessionKeys.sessionId -> sessionId,
          SessionKeys.authToken -> "mock-bearer-token",
          SessionValues.TAX_YEAR -> (if(isEoy) taxYearEOY else taxYear).toString,
          SessionValues.VALID_TAX_YEARS -> validTaxYears.mkString(","))),
          "mtditid" -> mtditid
        )
      }
    } ++
      Seq(xSessionId)
  }

  val defaultAcceptedConfidenceLevels: Seq[ConfidenceLevel] = Seq(
    ConfidenceLevel.L250,
    ConfidenceLevel.L500
  )

  def authService(stubbedRetrieval: Future[_], acceptedConfidenceLevel: Seq[ConfidenceLevel]): AuthService = new AuthService(
    new MockAuthConnector(stubbedRetrieval, acceptedConfidenceLevel)
  )

  def successfulRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L250
  )

  def insufficientConfidenceRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L50
  )

  def incorrectCredsRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L250
  )

  def userDataStub(userData: IncomeSourcesModel, nino: String, taxYear: Int): StubMapping = {
    stubGetWithHeadersCheck(
      s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", OK,
      Json.toJson(userData).toString(), "X-Session-ID" -> sessionId, "mtditid" -> mtditid)
  }

  def userDataStubWithError(nino: String, taxYear: Int): StubMapping = {
    stubGetWithHeadersCheck(
      s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", INTERNAL_SERVER_ERROR,
      "Error parsing response from API", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)
  }

  def emptyUserDataStub(nino: String = nino, taxYear: Int = taxYear): StubMapping = {
    userDataStub(IncomeSourcesModel(), nino, taxYear)
  }

  def stockDividendsUserDataStub(userData: Option[StockDividendsPriorSubmission], nino: String, taxYear: Int): StubMapping = {
    stubGetWithHeadersCheck(
      s"/income-tax-dividends/income-tax/income/dividends/${user.nino}/$taxYear", OK,
      Json.toJson(userData).toString(), "X-Session-ID" -> sessionId, "mtditid" -> mtditid)
  }

  def emptyStockDividendsUserDataStub(nino: String = nino, taxYear: Int = taxYear): StubMapping = {
    stubGetWithHeadersCheck(
      s"/income-tax-dividends/income-tax/income/dividends/${user.nino}/$taxYear", NOT_FOUND,
      "", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)
  }

  val CompleteStockDividendsUserData: StockDividendsUserDataModel = StockDividendsUserDataModel(
    "sessionId-1618a1e8-4979-41d8-a32e-5ffbe69fac81",
    "1234567890",
    "AA123456A",
    taxYear,
    Some(completeStockDividendsCYAModel)
  )

}
