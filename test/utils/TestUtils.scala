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

import com.codahale.metrics.SharedMetricRegistries
import common.{EnrolmentIdentifiers, EnrolmentKeys}
import config.{AppConfig, MockAppConfig}
import controllers.predicates.AuthorisedAction
import models.dividends.{DividendsCheckYourAnswersModel, StockDividendsCheckYourAnswersModel}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, DefaultActionBuilder, Result}
import play.api.test.{FakeRequest, Helpers}
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

trait TestUtils extends AnyWordSpec with Matchers with MockFactory with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    SharedMetricRegistries.clear()
  }

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = Materializer(actorSystem)

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("mtditid" -> "1234567890", SessionKeys.sessionId -> "some-session-id")
  val fakeRequestWithMtditid: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession("MTDITID" -> "1234567890")
  implicit val emptyHeaderCarrier: HeaderCarrier = HeaderCarrier()

  lazy val mockAppConfig: AppConfig = new MockAppConfig
  implicit val mockControllerComponents: ControllerComponents = Helpers.stubControllerComponents()
  implicit val mockExecutionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit val mockAuthConnector: AuthConnector = mock[AuthConnector]
  implicit val mockAuthService: AuthService = new AuthService(mockAuthConnector)
  val defaultActionBuilder: DefaultActionBuilder = DefaultActionBuilder(mockControllerComponents.parsers.default)
  val authorisedAction = new AuthorisedAction()(mockAuthConnector, defaultActionBuilder, mockControllerComponents)

  val nino: String = "123456789"
  val mtditid: String = "1234567890"
  val taxYear: Int = 1234

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

  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

  val individualEnrolments: Enrolments = Enrolments(Set(
    Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
    Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, "1234567890")), "Activated")))

  //noinspection ScalaStyle
  def mockAuth(enrolments: Enrolments = individualEnrolments): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.affinityGroup, *, *)
      .returning(Future.successful(Some(AffinityGroup.Individual)))

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
      .returning(Future.successful(enrolments and ConfidenceLevel.L250))
  }

  val agentEnrolments: Enrolments = Enrolments(Set(
    Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, "1234567890")), "Activated"),
    Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
  ))

  //noinspection ScalaStyle
  def mockAuthAsAgent(enrolments: Enrolments = agentEnrolments): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.affinityGroup, *, *)
      .returning(Future.successful(Some(AffinityGroup.Agent)))

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.allEnrolments, *, *)
      .returning(Future.successful(enrolments))
  }

  //noinspection ScalaStyle
  def mockAuthReturnException(exception: Exception): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {
    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(Future.failed(exception))
  }
}

