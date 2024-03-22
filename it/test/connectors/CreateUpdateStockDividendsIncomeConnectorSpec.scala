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
import config.BackendAppConfig
import helpers.WiremockSpec
import models._
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.TaxYearUtils.convertStringTaxYear

class CreateUpdateStockDividendsIncomeConnectorSpec extends WiremockSpec {

  lazy val connector: CreateUpdateStockDividendsIncomeConnector = app.injector.instanceOf[CreateUpdateStockDividendsIncomeConnector]

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(ifHost: String): BackendAppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override lazy val ifBaseUrl: String = s"http://$ifHost:$wireMockPort"
  }

  val nino: String = "123456789"
  val taxYear: Int = 2023
  val taxYearParameter: String = convertStringTaxYear(taxYear)
  val reference: String = "RefNo13254687"
  val countryCode: String = "GBR"
  val decimalValue: BigDecimal = 123.45
  val url = s"/income-tax/income/dividends/$nino/$taxYearParameter"
  val model: StockDividendsSubmissionModel = StockDividendsSubmissionModel(
    foreignDividend =
      Some(Seq(
        ForeignDividendModel(countryCode, Some(decimalValue), Some(decimalValue), Some(decimalValue), Some(true), decimalValue)
      ))
    ,
    dividendIncomeReceivedWhilstAbroad = Some(
      Seq(
        ForeignDividendModel(countryCode, Some(decimalValue), Some(decimalValue), Some(decimalValue), Some(true), decimalValue)
      )
    ),
    stockDividend = Some(StockDividendModel(Some(reference), decimalValue)),
    redeemableShares = Some(StockDividendModel(Some(reference), decimalValue)),
    bonusIssuesOfSecurities = Some(StockDividendModel(Some(reference), decimalValue)),
    closeCompanyLoansWrittenOff = Some(StockDividendModel(Some(reference), decimalValue))
  )

  val createOrAmendDividendsResponse: StockDividendsSubmissionModel = model
  val updateDividendsModel: StockDividendsSubmissionModel = model.copy(None, None, None, None, None, None)

  "CreateUpdateDividendsConnector" should {
    "include internal headers" when {
      val requestBody = Json.toJson(model).toString()
      val responseBody = Json.toJson(createOrAmendDividendsResponse).toString()

      val headersSent = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new CreateUpdateStockDividendsIncomeConnector(httpClient, appConfig(internalHost))

        stubPutWithResponseBody(url, NO_CONTENT, requestBody, responseBody, headersSent)

        val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

        result mustBe Right(true)
      }

      "the host is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new CreateUpdateStockDividendsIncomeConnector(httpClient, appConfig(externalHost))

        stubPutWithResponseBody(url, NO_CONTENT, requestBody, responseBody, headersSent)

        val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

        result mustBe Right(true)
      }
    }

    "return a success result" when {
      "Returns a 200" in {

        stubPutWithResponseBody(url, NO_CONTENT, Json.toJson(model).toString(), Json.toJson(model).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

        result mustBe Right(true)
      }
    }

    "return a Internal Server Error from" in {
      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "Internal server error"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("SERVER_ERROR", "Internal server error"))

      stubPutWithResponseBody(url, INTERNAL_SERVER_ERROR, Json.toJson(model).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Service Unavailable from" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubPutWithResponseBody(url, SERVICE_UNAVAILABLE, Json.toJson(model).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when throws an unexpected result that is parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubPutWithResponseBody(url, CONFLICT, Json.toJson(model).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }

  }
}
