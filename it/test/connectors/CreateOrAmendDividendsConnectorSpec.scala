/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class CreateOrAmendDividendsConnectorSpec extends WiremockSpec {

  lazy val connector: CreateOrAmendDividendsConnector = app.injector.instanceOf[CreateOrAmendDividendsConnector]

  lazy val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

  def appConfig(desHost: String): BackendAppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override val desBaseUrl: String = s"http://$desHost:$wireMockPort"
  }

  val nino: String = "123456789"
  val taxYear: Int = 1999
  val updateDividendsModel: CreateOrAmendDividendsModel = CreateOrAmendDividendsModel(Some(123.12), Some(321.21))
  val createOrAmendDividendsResponse: CreateOrAmendDividendsResponseModel = CreateOrAmendDividendsResponseModel("String")
  val dividendResult: Option[BigDecimal] = Some(123456.78)

  "CreateOrAmendDividendsConnector" should {

    "include internal headers" when {
      val requestBody = Json.toJson(updateDividendsModel).toString()
      val responseBody = Json.toJson(createOrAmendDividendsResponse).toString()

      val headersSentToDes = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new CreateOrAmendDividendsConnector(httpClient, appConfig(internalHost))
        val expectedResult = createOrAmendDividendsResponse

        stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, requestBody, responseBody, headersSentToDes)

        val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

        result mustBe Right(expectedResult)
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new CreateOrAmendDividendsConnector(httpClient, appConfig(externalHost))
        val expectedResult = createOrAmendDividendsResponse

        stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, requestBody, responseBody, headersSentToDes)

        val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a success result" when {
      "DES Returns a 200" in {
        val expectedResult = createOrAmendDividendsResponse

        stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, Json.toJson(updateDividendsModel).toString(), Json.toJson(createOrAmendDividendsResponse).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a Parsing error INTERNAL_SERVER_ERROR response" in {
      val invalidJson = Json.obj(
        "ukDividends" -> ""
      )

      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, Json.toJson(updateDividendsModel).toString(), invalidJson.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NOT_FOUND response" in {
      val responseBody = Json.obj(
        "code" -> "NOT_FOUND",
        "reason" -> "Submission Period not found"
      )
      val expectedResult = ErrorModel(404, ErrorBodyModel("NOT_FOUND", "Submission Period not found"))

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", NOT_FOUND, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return an UNPROCESSABLE_ENTITY response" in {
      val responseBody = Json.obj(
        "code" -> "UNPROCESSABLE_ENTITY",
        "reason" -> "The remote endpoint has indicated that for given income source type, message payload is incorrect."
      )
      val expectedResult = ErrorModel(422, ErrorBodyModel("UNPROCESSABLE_ENTITY", "The remote endpoint has indicated that for given income source type, message payload is incorrect."))

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", UNPROCESSABLE_ENTITY, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Forbidden" in {
      val responseBody = Json.obj(
        "code" -> "NOT_FOUND_INCOME_SOURCE",
        "reason" -> "Can't find income source"
      )
      val expectedResult = ErrorModel(403, ErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source"))

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", FORBIDDEN, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Internal Server Error" in {
      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "Internal server error"
      )
      val expectedResult = ErrorModel(500, ErrorBodyModel("SERVER_ERROR", "Internal server error"))

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", INTERNAL_SERVER_ERROR, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Service Unavailable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = ErrorModel(503, ErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", SERVICE_UNAVAILABLE, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result with no body" in {
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubPostWithoutResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", NO_CONTENT, Json.toJson(updateDividendsModel).toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result that is parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", CONFLICT, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result that isn't parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", CONFLICT, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

  }
}
