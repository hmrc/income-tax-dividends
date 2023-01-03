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
import models.{CreateOrAmendDividendsModel, CreateOrAmendDividendsResponseModel, ErrorBodyModel, ErrorModel}
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class CreateUpdateAnnualIncomeSourceConnectorSpec extends WiremockSpec {

  lazy val connector: CreateUpdateAnnualIncomeSourceConnector = app.injector.instanceOf[CreateUpdateAnnualIncomeSourceConnector]

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(ifHost: String): BackendAppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override lazy val ifBaseUrl: String = s"http://$ifHost:$wireMockPort"
  }

  val nino: String = "AB345678C"
  val taxYear: Int = 2024
  //val incomeSourceType = "dividends"
  val formattedTaxYear: String = "23-24"
  val updateAnnualIncomeSourceModel: CreateOrAmendDividendsModel = CreateOrAmendDividendsModel(Some(123.12), Some(321.21))
  val createUpdateAnnualIncomeSourceResponse: CreateOrAmendDividendsResponseModel = CreateOrAmendDividendsResponseModel("transactionRef")
  val dividendResult: Option[BigDecimal] = Some(123456.78)

  "CreateUpdateAnnualIncomeSourceConnector" should {

    "include internal headers" when {
      val requestBody = Json.toJson(updateAnnualIncomeSourceModel).toString()
      val responseBody = Json.toJson(createUpdateAnnualIncomeSourceResponse).toString()

      val headersSentToIf = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"),
      )

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for IF is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new CreateUpdateAnnualIncomeSourceConnector(httpClient, appConfig(internalHost))
        val expectedResult = createUpdateAnnualIncomeSourceResponse

        stubPostWithResponseBody(s"/income-tax/$formattedTaxYear/$nino/income-source/dividends/annual", OK, requestBody, responseBody, headersSentToIf)

        val result = await(connector.createUpdateAnnualIncomeSource(nino, taxYear, updateAnnualIncomeSourceModel)(hc))

        result mustBe Right(expectedResult)
      }

      "the host for IF is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new CreateUpdateAnnualIncomeSourceConnector(httpClient, appConfig(externalHost))
        val expectedResult = createUpdateAnnualIncomeSourceResponse

        stubPostWithResponseBody(s"/income-tax/$formattedTaxYear/$nino/income-source/dividends/annual", OK, requestBody, responseBody, headersSentToIf)

        val result = await(connector.createUpdateAnnualIncomeSource(nino, taxYear, updateAnnualIncomeSourceModel)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a success result" when {
      "IF Returns a 200" in {
        val expectedResult = createUpdateAnnualIncomeSourceResponse

        stubPostWithResponseBody(
          s"/income-tax/$formattedTaxYear/$nino/income-source/dividends/annual",
          OK,
          Json.toJson(updateAnnualIncomeSourceModel).toString(),
          Json.toJson(createUpdateAnnualIncomeSourceResponse).toString()
        )

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createUpdateAnnualIncomeSource(nino, taxYear, updateAnnualIncomeSourceModel)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a Parsing error INTERNAL_SERVER_ERROR response" in {
      val invalidJson = Json.obj(
        "ukDividends" -> ""
      )

      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubPostWithResponseBody(
        s"/income-tax/$formattedTaxYear/$nino/income-source/dividends/annual",
        OK,
        Json.toJson(updateAnnualIncomeSourceModel).toString(),
        invalidJson.toString()
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateAnnualIncomeSource(nino, taxYear, updateAnnualIncomeSourceModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NOT_FOUND response" in {
      val responseBody = Json.obj(
        "code" -> "NOT_FOUND",
        "reason" -> "Submission Period not found"
      )
      val expectedResult = ErrorModel(NOT_FOUND, ErrorBodyModel("NOT_FOUND", "Submission Period not found"))

      stubPostWithResponseBody(
        s"/income-tax/$formattedTaxYear/$nino/income-source/dividends/annual",
        NOT_FOUND,
        Json.toJson(updateAnnualIncomeSourceModel).toString(),
        responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateAnnualIncomeSource(nino, taxYear, updateAnnualIncomeSourceModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return an UNPROCESSABLE_ENTITY response" in {
      val responseBody = Json.obj(
        "code" -> "UNPROCESSABLE_ENTITY",
        "reason" -> "The remote endpoint has indicated that for given income source type, message payload is incorrect."
      )
      val expectedResult = ErrorModel(
        UNPROCESSABLE_ENTITY,
        ErrorBodyModel(
          "UNPROCESSABLE_ENTITY",
          "The remote endpoint has indicated that for given income source type, message payload is incorrect."
        )
      )

      stubPostWithResponseBody(
        s"/income-tax/$formattedTaxYear/$nino/income-source/dividends/annual",
        UNPROCESSABLE_ENTITY,
        Json.toJson(updateAnnualIncomeSourceModel).toString(),
        responseBody.toString()
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateAnnualIncomeSource(nino, taxYear, updateAnnualIncomeSourceModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Forbidden" in {
      val responseBody = Json.obj(
        "code" -> "NOT_FOUND_INCOME_SOURCE",
        "reason" -> "Can't find income source"
      )
      val expectedResult = ErrorModel(FORBIDDEN, ErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source"))

      stubPostWithResponseBody(
        s"/income-tax/$formattedTaxYear/$nino/income-source/dividends/annual",
        FORBIDDEN,
        Json.toJson(updateAnnualIncomeSourceModel).toString(),
        responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateAnnualIncomeSource(nino, taxYear, updateAnnualIncomeSourceModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Internal Server Error" in {
      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "Internal server error"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("SERVER_ERROR", "Internal server error"))

      stubPostWithResponseBody(
        s"/income-tax/$formattedTaxYear/$nino/income-source/dividends/annual",
        INTERNAL_SERVER_ERROR, Json.toJson(updateAnnualIncomeSourceModel).toString(),
        responseBody.toString()
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateAnnualIncomeSource(nino, taxYear, updateAnnualIncomeSourceModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Service Unavailable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubPostWithResponseBody(
        s"/income-tax/$formattedTaxYear/$nino/income-source/dividends/annual",
        SERVICE_UNAVAILABLE,
        Json.toJson(updateAnnualIncomeSourceModel).toString(),
        responseBody.toString()
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateAnnualIncomeSource(nino, taxYear, updateAnnualIncomeSourceModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when IF throws an unexpected result with no body" in {
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubPostWithoutResponseBody(
        s"/income-tax/$formattedTaxYear/$nino/income-source/dividends/annual",
        NO_CONTENT,
        Json.toJson(updateAnnualIncomeSourceModel).toString()
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateAnnualIncomeSource(nino, taxYear, updateAnnualIncomeSourceModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when IF throws an unexpected result that is parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubPostWithResponseBody(
        s"/income-tax/$formattedTaxYear/$nino/income-source/dividends/annual",
        CONFLICT,
        Json.toJson(updateAnnualIncomeSourceModel).toString(),
        responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateAnnualIncomeSource(nino, taxYear, updateAnnualIncomeSourceModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when IF throws an unexpected result that isn't parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubPostWithResponseBody(
        s"/income-tax/$formattedTaxYear/$nino/income-source/dividends/annual",
        CONFLICT, Json.toJson(updateAnnualIncomeSourceModel).toString(),
        responseBody.toString()
      )
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createUpdateAnnualIncomeSource(nino, taxYear, updateAnnualIncomeSourceModel)(hc))

      result mustBe Left(expectedResult)
    }
  }
}

