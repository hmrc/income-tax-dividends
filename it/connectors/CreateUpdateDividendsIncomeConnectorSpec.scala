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

class CreateUpdateDividendsIncomeConnectorSpec extends WiremockSpec {

  lazy val connector: CreateUpdateDividendsIncomeConnector = app.injector.instanceOf[CreateUpdateDividendsIncomeConnector]

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(desHost: String): BackendAppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override val desBaseUrl: String = s"http://$desHost:$wireMockPort"
  }

  val nino: String = "123456789"
  val taxYear: Int = 2023
  val taxYearParameter: String = convertStringTaxYear(taxYear)
  val reference: String = "RefNo13254687"
  val countryCode: String = "GBR"
  val decimalValue: BigDecimal = 123.45
  val url = s"/income-tax/income/dividends/$nino/$taxYearParameter"
  val model: DividendsIncomeDataModel = DividendsIncomeDataModel(
    submittedOn = None,
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

  val createOrAmendDividendsResponse: DividendsIncomeDataModel = model
  val updateDividendsModel: DividendsIncomeDataModel = model.copy(None, None, None, None, None, None)

  "CreateUpdateDividendsConnector" should {
    "include internal headers" when {
      val requestBody = Json.toJson(model).toString()
      val responseBody = Json.toJson(createOrAmendDividendsResponse).toString()

      val headersSentT = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new CreateUpdateDividendsIncomeConnector(httpClient, appConfig(internalHost))
        val expectedResult = model

        stubPostWithResponseBody(url, OK, requestBody, responseBody, headersSentT)

        val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

        result mustBe Right(expectedResult)
      }

      "the host is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new CreateUpdateDividendsIncomeConnector(httpClient, appConfig(externalHost))
        val expectedResult = model

        stubPostWithResponseBody(url, OK, requestBody, responseBody, headersSentT)

        val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a success result" when {
      "Returns a 200" in {
        val expectedResult = model

        stubPostWithResponseBody(url, OK, Json.toJson(model).toString(), Json.toJson(model).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a Parsing error INTERNAL_SERVER_ERROR response from" in {
      val invalidJson = Json.obj(
        "submittedOn" -> "",
        "foreignDividend" -> "",
        "dividendIncomeReceivedWhilstAbroad" -> "",
        "stockDividend" -> "",
        "redeemableShares" -> "",
        "bonusIssuesOfSecurities" -> "",
        "closeCompanyLoansWrittenOff" -> ""
      )

      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubPostWithResponseBody(url , OK, Json.toJson(model).toString(), invalidJson.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NOT_FOUND response from" in {
      val responseBody = Json.obj(
        "code" -> "NOT_FOUND",
        "reason" -> "Submission Period not found"
      )
      val expectedResult = ErrorModel(NOT_FOUND, ErrorBodyModel("NOT_FOUND", "Submission Period not found"))

      stubPostWithResponseBody(url, NOT_FOUND, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }

    "return an UNPROCESSABLE_ENTITY response from" in {
      val responseBody = Json.obj(
        "code" -> "UNPROCESSABLE_ENTITY",
        "reason" -> "The remote endpoint has indicated that for given income source type, message payload is incorrect."
      )
      val expectedResult = ErrorModel(
        UNPROCESSABLE_ENTITY,
        ErrorBodyModel("UNPROCESSABLE_ENTITY", "The remote endpoint has indicated that for given income source type, message payload is incorrect."))

      stubPostWithResponseBody(url, UNPROCESSABLE_ENTITY, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Forbidden from" in {
      val responseBody = Json.obj(
        "code" -> "NOT_FOUND_INCOME_SOURCE",
        "reason" -> "Can't find income source"
      )
      val expectedResult = ErrorModel(FORBIDDEN, ErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source"))

      stubPostWithResponseBody(url, FORBIDDEN, Json.toJson(model).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Internal Server Error from" in {
      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "Internal server error"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("SERVER_ERROR", "Internal server error"))

      stubPostWithResponseBody(url, INTERNAL_SERVER_ERROR, Json.toJson(model).toString(), responseBody.toString())
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

      stubPostWithResponseBody(url, SERVICE_UNAVAILABLE, Json.toJson(model).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when throws an unexpected result with no body" in {
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubPostWithoutResponseBody(url, NO_CONTENT, Json.toJson(model).toString())
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

      stubPostWithResponseBody(url, CONFLICT, Json.toJson(model).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when throws an unexpected result that isn't parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubPostWithResponseBody(url, CONFLICT, Json.toJson(model).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateDividends(nino, taxYear, model)(hc))

      result mustBe Left(expectedResult)
    }
  }
}
