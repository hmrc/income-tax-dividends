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

package controllers

import connectors.httpParsers.CreateUpdateStockDividendsIncomeHttpParser.CreateUpdateStockDividendsIncomeResponse
import models.dividends.StockDividendsCheckYourAnswersModel
import models.{CreateOrAmendDividendsResponseModel, ErrorBodyModel, ErrorModel}
import org.scalamock.handlers.CallHandler4
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import services.StockDividendsSessionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class CreateStockDividendsSessionDataControllerSpec extends TestUtils {

  val stockDividendsSessionService: StockDividendsSessionService = mock[StockDividendsSessionService]
  val createStockDividendsSessionDataController = new CreateStockDividendsSessionDataController(stockDividendsSessionService,
    mockControllerComponents, authorisedAction)
  val nino: String = "123456789"
  val mtditid: String = "1234567890"
  val taxYear: Int = 1234
  val badRequestModel: ErrorBodyModel = ErrorBodyModel("INVALID_NINO", "Nino is invalid")
  val notFoundModel: ErrorBodyModel = ErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source")
  val serverErrorModel: ErrorBodyModel = ErrorBodyModel("SERVER_ERROR", "Internal server error")
  val serviceUnavailableErrorModel: ErrorBodyModel = ErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable")
  private val fakeGetRequest = FakeRequest("PUT", "/").withHeaders("mtditid" -> mtditid)
  private val fakeGetRequestWithDifferentMTITID = FakeRequest("PUT", "/").withHeaders("mtditid" -> "123123123")

  val jsonBody: JsValue = Json.parse("""{"ukDividends": 12345.99, "otherUkDividends": 123456.99}""")
  val invalidJsonBody: JsValue = Json.parse("""{"notukDividends": 12345.99, "nototherUkDividends": 123456.99}""")

  def mockCreateStockDividendsSessionDataValid(): CallHandler4[String, Int, StockDividendsCheckYourAnswersModel,
    HeaderCarrier, Future[CreateUpdateStockDividendsIncomeResponse]] = {
    val response: CreateUpdateStockDividendsIncomeResponse = Right(CreateOrAmendDividendsResponseModel("transactionRef"))
    (stockDividendsSessionService.createSessionData(_: StockDividendsCheckYourAnswersModel, _: Int)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }

  def mockCreateStockDividendsSessionDataBadRequest(): CallHandler4[String, Int, StockDividendsCheckYourAnswersModel, HeaderCarrier,
    Future[CreateUpdateStockDividendsIncomeResponse]] = {
    val response: CreateUpdateStockDividendsIncomeResponse = Left(ErrorModel(BAD_REQUEST, badRequestModel))

    (stockDividendsSessionService.createSessionData(_: StockDividendsCheckYourAnswersModel, _: Int)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }

  def mockCreateStockDividendsSessionDataNotFound(): CallHandler4[String, Int, StockDividendsCheckYourAnswersModel,
    HeaderCarrier, Future[CreateUpdateStockDividendsIncomeResponse]] = {
    val response = Left(ErrorModel(NOT_FOUND, notFoundModel))
    (stockDividendsSessionService.createSessionData(_: StockDividendsCheckYourAnswersModel, _: Int)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }

  def mockCreateStockDividendsSessionDataServerError(): CallHandler4[String, Int, StockDividendsCheckYourAnswersModel,
    HeaderCarrier, Future[CreateUpdateStockDividendsIncomeResponse]] = {
    val response = Left(ErrorModel(INTERNAL_SERVER_ERROR, serverErrorModel))
    (stockDividendsSessionService.createSessionData(_: StockDividendsCheckYourAnswersModel, _: Int)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }

  def mockCreateStockDividendsSessionDataServiceUnavailable()
  : CallHandler4[String, Int, StockDividendsCheckYourAnswersModel, HeaderCarrier, Future[CreateUpdateStockDividendsIncomeResponse]] = {
    val response = Left(ErrorModel(SERVICE_UNAVAILABLE, serviceUnavailableErrorModel))
    (stockDividendsSessionService.createSessionData(_: StockDividendsCheckYourAnswersModel, _: Int)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }

  "calling .createSessionData" should {

    "return a 204 No Content response when called as an individual" in {
      val result = {
        mockAuth()
        mockCreateStockDividendsSessionDataValid()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe NO_CONTENT
    }

    "return a 401 when called as an individual" in {
      val result = {
        mockAuth()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequestWithDifferentMTITID.withJsonBody(jsonBody))
      }
      status(result) mustBe UNAUTHORIZED
    }

    "return a 204 No Content response when called as an agent" in {
      val result = {
        mockAuthAsAgent()
        mockCreateStockDividendsSessionDataValid()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe NO_CONTENT
    }

    "return an badRequest response when called as an individual with an invalid request body" in {
      val result = {
        mockAuth()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequest.withJsonBody(invalidJsonBody))
      }
      status(result) mustBe BAD_REQUEST
    }

    "return a badRequest response when called as an individual and DES returns badRequest" in {
      val result = {
        mockAuth()
        mockCreateStockDividendsSessionDataBadRequest()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe BAD_REQUEST
    }

    "return an badRequest response when called as an agent and DES returns badRequest" in {
      val result = {
        mockAuthAsAgent()
        mockCreateStockDividendsSessionDataBadRequest()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe BAD_REQUEST
    }

    "return a notFound response when called as an individual and DES returns badRequest" in {
      val result = {
        mockAuth()
        mockCreateStockDividendsSessionDataNotFound()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe NOT_FOUND
    }

    "return an notFound response when called as an agent and DES returns badRequest" in {
      val result = {
        mockAuthAsAgent()
        mockCreateStockDividendsSessionDataNotFound()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe NOT_FOUND
    }

    "return an InternalServerError response when called as an individual" in {
      val result = {
        mockAuth()
        mockCreateStockDividendsSessionDataServerError()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return an InternalServerError response when called as an agent" in {
      val result = {
        mockAuthAsAgent()
        mockCreateStockDividendsSessionDataServerError()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return an ServiceUnavailable response when called as an individual" in {
      val result = {
        mockAuth()
        mockCreateStockDividendsSessionDataServiceUnavailable()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe SERVICE_UNAVAILABLE
    }

    "return an ServiceUnavailable response when called as an agent" in {
      val result = {
        mockAuthAsAgent()
        mockCreateStockDividendsSessionDataServiceUnavailable()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe SERVICE_UNAVAILABLE
    }
  }
}
