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
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.TaxYearUtils
import utils.TaxYearUtils.convertSpecificTaxYear

class GetAnnualIncomeSourcePeriodConnectorSpec extends WiremockSpec {

  lazy val connector: GetAnnualIncomeSourcePeriodConnector = app.injector.instanceOf[GetAnnualIncomeSourcePeriodConnector]

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(ifHost: String): BackendAppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override lazy val ifBaseUrl: String = s"http://$ifHost:$wireMockPort"
  }

  val dividendResult: Option[BigDecimal] = Some(123456.78)
  val expectedResult = SubmittedDividendsModel(dividendResult, dividendResult)
  val responseBody: String = Json.obj(
    "ukDividendsAnnual" -> expectedResult
  ).toString()

  val nino: String = "123456789"

  val specificTaxYear: Int = TaxYearUtils.specificTaxYear
  val specificTaxYearPlusOne: Int = specificTaxYear + 1
  val taxYearParameter: String = convertSpecificTaxYear(specificTaxYear)
  val taxYearParameterPlusOne: String = convertSpecificTaxYear(specificTaxYearPlusOne)
  val deletedPeriod: Option[Boolean] = Some(false)
  val url: String = s"/income-tax/$taxYearParameter/$nino/income-source/dividends/annual\\?deleteReturnPeriod=false"
  val urlPlusOne: String = s"/income-tax/$taxYearParameterPlusOne/$nino/income-source/dividends/annual\\?deleteReturnPeriod=false"

  "GetAnnualIncomeSourcePeriodConnector" should {

    "include internal headers" when {

      val headersSentToDes = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for IF is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new GetAnnualIncomeSourcePeriodConnector(httpClient, appConfig(internalHost))
        stubGetWithResponseBody(url, OK, responseBody, headersSentToDes)

        val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

        result mustBe Right(expectedResult)
      }

      "the host for IF is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new GetAnnualIncomeSourcePeriodConnector(httpClient, appConfig(externalHost))
        stubGetWithResponseBody(url, OK, responseBody, headersSentToDes)

        val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a SubmittedDividendsModel" when {
      "IF returns a 200 for specific tax year" in {

        stubGetWithResponseBody(url, OK, responseBody)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

        result mustBe Right(expectedResult)
      }

      "IF returns a 200 for specific tax year plus one" in {

        stubGetWithResponseBody(urlPlusOne, OK, responseBody)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYearPlusOne, deletedPeriod)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "DES Returns multiple errors" in {
      val expectedResult = ErrorModel(BAD_REQUEST, ErrorsBodyModel(Seq(
        ErrorBodyModel("INVALID_IDTYPE", "ID is invalid"),
        ErrorBodyModel("INVALID_IDTYPE_2", "ID 2 is invalid"))))

      val responseBody = Json.obj(
        "failures" -> Json.arr(
          Json.obj("code" -> "INVALID_IDTYPE",
            "reason" -> "ID is invalid"),
          Json.obj("code" -> "INVALID_IDTYPE_2",
            "reason" -> "ID 2 is invalid")
        )
      )
      stubGetWithResponseBody(url, BAD_REQUEST, responseBody.toString())

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Parsing error INTERNAL_SERVER_ERROR response" in {
      val invalidJson = Json.obj(
        "ukDividends" -> ""
      )

      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubGetWithResponseBody(url, OK, invalidJson.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NO_CONTENT" in {
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubGetWithResponseBody(url, NO_CONTENT, "{}")
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Bad Request" in {
      val responseBody = Json.obj(
        "code" -> "INVALID_NINO",
        "reason" -> "Nino is invalid"
      )
      val expectedResult = ErrorModel(BAD_REQUEST, ErrorBodyModel("INVALID_NINO", "Nino is invalid"))

      stubGetWithResponseBody(url, BAD_REQUEST, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Not found" in {
      val responseBody = Json.obj(
        "code" -> "NOT_FOUND_INCOME_SOURCE",
        "reason" -> "Can't find income source"
      )
      val expectedResult = ErrorModel(NOT_FOUND, ErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source"))

      stubGetWithResponseBody(url, NOT_FOUND, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal server error" in {
      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "Internal server error"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("SERVER_ERROR", "Internal server error"))

      stubGetWithResponseBody(url, INTERNAL_SERVER_ERROR, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Service Unavailable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubGetWithResponseBody(url, SERVICE_UNAVAILABLE, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when IF throws an unexpected result" in {
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(url, INTERNAL_SERVER_ERROR)
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when IF throws an unexpected result that is parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubGetWithResponseBody(url, CONFLICT, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when IF throws an unexpected result that isn't parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE"
      )
      val expectedResult = ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError)

      stubGetWithResponseBody(url, CONFLICT, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Unprocessable entity Error when IF throws an unexpected result that isn't parsable" in {
      val responseBody = Json.obj(
        "code" -> "UNPROCESSABLE_ENTITY"
      )
      val expectedResult = ErrorModel(UNPROCESSABLE_ENTITY, ErrorBodyModel.parsingError)

      stubGetWithResponseBody(url, UNPROCESSABLE_ENTITY, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getAnnualIncomeSourcePeriod(nino, specificTaxYear, deletedPeriod)(hc))

      result mustBe Left(expectedResult)
    }
  }
}
