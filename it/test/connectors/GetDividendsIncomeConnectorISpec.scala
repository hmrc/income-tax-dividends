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

package connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, BackendAppConfig}
import helpers.WiremockSpec
import models._
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.TaxYearUtils.convertStringTaxYear

class GetDividendsIncomeConnectorISpec extends PlaySpec with WiremockSpec {

  lazy val connector: GetDividendsIncomeConnector = app.injector.instanceOf[GetDividendsIncomeConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val nino = "nino"
  val taxYear = 2020
  val taxYearParameter: String = convertStringTaxYear(taxYear)
  val url = s"/income-tax/income/dividends/$nino/$taxYearParameter"

  lazy val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

  def appConfig(ifHost: String): AppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override lazy val ifBaseUrl: String = s"http://$ifHost:$wireMockPort"
  }

  val ifReturned: DividendsIncomeDataModel = DividendsIncomeDataModel(
    submittedOn = Some("2020-06-17T10:53:38Z"),
    foreignDividend = Some(Seq(ForeignDividendModel("BES", Some(2323.56), Some(5435.56), Some(4564.67), Some(true), 4564.67))),
    dividendIncomeReceivedWhilstAbroad = Some(Seq(ForeignDividendModel("CHN", Some(5664.67), Some(5657.56),
      Some(4644.56), Some(true), 4654.56))),
    stockDividend = Some(StockDividendModel(Some("Stock Dividend Customer Reference"), 2525.89)),
    redeemableShares = Some(StockDividendModel(Some("Redeemable Shares Customer Reference"), 3535.56)),
    bonusIssuesOfSecurities = Some(StockDividendModel(Some("Bonus Issues Of Securities Customer Reference"), 5633.67)),
    closeCompanyLoansWrittenOff = Some(StockDividendModel(Some("Close Company Loans WrittenOff Customer Reference"), 6743.23))
  )

  " GetDividendsIncomeDataConnector" should {

    "include internal headers" when {

      val headersSentToDes = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val externalHost = "127.0.0.1"

      "the host for IF is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(url, OK, Json.toJson(ifReturned).toString)

        val result = await(connector.getDividendsIncomeData(nino, taxYear)(hc))

        result mustBe Right(ifReturned)
      }

      "the host for IF is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

        stubGetWithResponseBody(url, OK, Json.toJson(ifReturned).toString, headersSentToDes)

        val connector = new GetDividendsIncomeConnector(httpClient, appConfig(externalHost))

        val result = await(connector.getDividendsIncomeData(nino, taxYear)(hc))

        result mustBe Right(ifReturned)
      }
    }

    "return a success result" when {

      "IF returns a 200" in {
        stubGetWithResponseBody(url, OK, Json.toJson(ifReturned).toString)
        val result = await(connector.getDividendsIncomeData(nino, taxYear))

        result mustBe Right(ifReturned)

      }
    }

    "return a NoContent response" in {

      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)
      stubGetWithResponseBody(url, NO_CONTENT, "{}")

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getDividendsIncomeData(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a BadRequest response" in {

      val responseBody = Json.obj(
        "code" -> "INVALID_NINO",
        "reason" -> "NINO is invalid"
      )
      val expectedResult = ErrorModel(BAD_REQUEST, ErrorBodyModel("INVALID_NINO", "NINO is invalid"))
      stubGetWithResponseBody(url, BAD_REQUEST, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getDividendsIncomeData(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NotFound response" in {

      val responseBody = Json.obj(
        "code" -> "NOT_FOUND_INCOME_SOURCE",
        "reason" -> "Can't find the income source"
      )
      val expectedResult = ErrorModel(NOT_FOUND, ErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find the income source"))
      stubGetWithResponseBody(url, NOT_FOUND, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getDividendsIncomeData(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an InternalServerError response" in {

      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "Internal Server Error"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("SERVER_ERROR", "Internal Server Error"))
      stubGetWithResponseBody(url, INTERNAL_SERVER_ERROR, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getDividendsIncomeData(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a ServiceUnavailable response" in {

      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "The service is currently unavailable"
      )
      val expectedResult = ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("SERVICE_UNAVAILABLE", "The service is currently unavailable"))
      stubGetWithResponseBody(url, SERVICE_UNAVAILABLE, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getDividendsIncomeData(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }
  }

}
