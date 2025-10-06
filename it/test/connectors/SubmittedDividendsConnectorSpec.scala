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
import models.{ErrorBodyModel, ErrorModel, ErrorsBodyModel, SubmittedDividendsModel}
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class SubmittedDividendsConnectorSpec extends WiremockSpec {

  lazy val connector: SubmittedDividendsConnector = app.injector.instanceOf[SubmittedDividendsConnector]

  lazy val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]
  def appConfig(desHost: String): BackendAppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override val desBaseUrl: String = s"http://$desHost:$wireMockPort"
  }

  val dividendResult: Option[BigDecimal] = Some(123456.78)
  val expectedResult = SubmittedDividendsModel(dividendResult, dividendResult)
  val responseBody: String = Json.toJson(expectedResult).toString()

  val nino: String = "123456789"
  val taxYear: Int = 1999

  ".SubmittedDividendsConnector" should {

    "include internal headers" when {

      val headersSentToDes = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new SubmittedDividendsConnector(httpClient, appConfig(internalHost))
        stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, responseBody, headersSentToDes)

        val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

        result mustBe Right(expectedResult)
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new SubmittedDividendsConnector(httpClient, appConfig(externalHost))
        stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, responseBody, headersSentToDes)

        val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a SubmittedDividendsModel" when {
      "all values are present" in {

        stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, responseBody)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "DES Returns multiple errors" in {
      val expectedResult = ErrorModel(BAD_REQUEST, ErrorsBodyModel(Seq(
        ErrorBodyModel("INVALID_IDTYPE","ID is invalid"),
        ErrorBodyModel("INVALID_IDTYPE_2","ID 2 is invalid"))))

      val responseBody = Json.obj(
        "failures" -> Json.arr(
          Json.obj("code" -> "INVALID_IDTYPE",
            "reason" -> "ID is invalid"),
          Json.obj("code" -> "INVALID_IDTYPE_2",
            "reason" -> "ID 2 is invalid")
        )
      )
      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", BAD_REQUEST, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Parsing error INTERNAL_SERVER_ERROR response" in {
      val invalidJson = Json.obj(
        "ukDividends" -> ""
      )

      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, invalidJson.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NO_CONTENT" in {
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", NO_CONTENT, "{}")
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Bad Request" in {
      val responseBody = Json.obj(
        "code" -> "INVALID_NINO",
        "reason" -> "Nino is invalid"
      )
      val expectedResult = ErrorModel(400, ErrorBodyModel("INVALID_NINO", "Nino is invalid"))

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", BAD_REQUEST, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Not found" in {
      val responseBody = Json.obj(
        "code" -> "NOT_FOUND_INCOME_SOURCE",
        "reason" -> "Can't find income source"
      )
      val expectedResult = ErrorModel(404, ErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source"))

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", NOT_FOUND, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal server error" in {
      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "Internal server error"
      )
      val expectedResult = ErrorModel(500, ErrorBodyModel("SERVER_ERROR", "Internal server error"))

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", INTERNAL_SERVER_ERROR, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Service Unavailable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = ErrorModel(503, ErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", SERVICE_UNAVAILABLE, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result" in {
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", NO_CONTENT)
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result that is parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR,  ErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", CONFLICT, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result that isn't parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR,  ErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", CONFLICT, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }
  }
}
