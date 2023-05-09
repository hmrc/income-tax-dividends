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
import models.{DividendsIncomeDataModel, ErrorBodyModel, ErrorModel, ForeignDividendModel, StockDividendModel, StockDividendsSubmissionModel}
import org.scalamock.handlers.CallHandler4
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import services.CreateUpdateDividendsIncomeService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class CreateUpdateDividendsIncomeControllerSpec extends TestUtils {

  val createUpdateDividendsIncomeService: CreateUpdateDividendsIncomeService = mock[CreateUpdateDividendsIncomeService]
  val createUpdateDividendsIncomeController =
    new CreateUpdateDividendsIncomeController(createUpdateDividendsIncomeService, mockControllerComponents, authorisedAction)
  val nino: String = "123456789"
  val mtditid: String = "1234567890"
  val taxYear: Int = 1234
  val reference: String = "RefNo13254687"
  val countryCode: String = "GBR"
  val decimalValue: BigDecimal = 123.45
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

  val badRequestModel: ErrorBodyModel = ErrorBodyModel("INVALID_NINO", "Nino is invalid")
  val notFoundModel: ErrorBodyModel = ErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source")
  val serverErrorModel: ErrorBodyModel = ErrorBodyModel("SERVER_ERROR", "Internal server error")
  val serviceUnavailableErrorModel: ErrorBodyModel = ErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable")
  private val fakeGetRequest = FakeRequest("POST", "/").withHeaders("mtditid" -> mtditid)
  private val fakeGetRequestWithDifferentMTITID = FakeRequest("POST", "/").withHeaders("mtditid" -> "123123123")

  val jsonBody: JsValue = Json.toJson(model)

  val invalidJsonBody: JsValue = Json.obj(
    "submittedOn" -> "",
    "foreignDividend" -> "",
    "dividendIncomeReceivedWhilstAbroad" -> "",
    "stockDividend" -> "",
    "redeemableShares" -> "",
    "bonusIssuesOfSecurities" -> "",
    "closeCompanyLoansWrittenOff" -> ""
  )

  def mockCreateUpdateDividendsIncomeValid(): CallHandler4
    [String, Int, StockDividendsSubmissionModel, HeaderCarrier, Future[CreateUpdateStockDividendsIncomeResponse]] = {
    val response: CreateUpdateStockDividendsIncomeResponse = Right(true)
    (createUpdateDividendsIncomeService.createUpdateDividends(_: String, _: Int, _: StockDividendsSubmissionModel)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }

  def mockCreateUpdateDividendsIncomeBadRequest(): CallHandler4
    [String, Int, StockDividendsSubmissionModel, HeaderCarrier, Future[CreateUpdateStockDividendsIncomeResponse]] = {
    val response: CreateUpdateStockDividendsIncomeResponse = Left(ErrorModel(BAD_REQUEST, badRequestModel))
    (createUpdateDividendsIncomeService.createUpdateDividends(_: String, _: Int, _: StockDividendsSubmissionModel)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }

  def mockCreateUpdateDividendsIncomeNotFound(): CallHandler4[String, Int, StockDividendsSubmissionModel, HeaderCarrier, Future[CreateUpdateStockDividendsIncomeResponse]] = {
    val response = Left(ErrorModel(NOT_FOUND, notFoundModel))
    (createUpdateDividendsIncomeService.createUpdateDividends(_: String, _: Int, _: StockDividendsSubmissionModel)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }

  def mockCreateUpdateDividendsIncomeServerError(): CallHandler4[String, Int, StockDividendsSubmissionModel, HeaderCarrier, Future[CreateUpdateStockDividendsIncomeResponse]] = {
    val response = Left(ErrorModel(INTERNAL_SERVER_ERROR, serverErrorModel))
    (createUpdateDividendsIncomeService.createUpdateDividends(_: String, _: Int, _: StockDividendsSubmissionModel)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }

  def mockCreateUpdateDividendsIncomeServiceUnavailable()
  : CallHandler4[String, Int, StockDividendsSubmissionModel, HeaderCarrier, Future[CreateUpdateStockDividendsIncomeResponse]] = {
    val response = Left(ErrorModel(SERVICE_UNAVAILABLE, serviceUnavailableErrorModel))
    (createUpdateDividendsIncomeService.createUpdateDividends(_: String, _: Int, _: StockDividendsSubmissionModel)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }

  "calling .createUpdateDividends" should {

    "return a 204 No Content response when called as an individual" in {
      val result = {
        mockAuth()
        mockCreateUpdateDividendsIncomeValid()
        createUpdateDividendsIncomeController.createUpdateDividends(nino, taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe NO_CONTENT
    }

    "return a 401 when called as an individual" in {
      val result = {
        mockAuth()
        createUpdateDividendsIncomeController.createUpdateDividends(nino, taxYear)(fakeGetRequestWithDifferentMTITID.withJsonBody(jsonBody))
      }
      status(result) mustBe UNAUTHORIZED
    }

    "return a 204 No Content response when called as an agent" in {
      val result = {
        mockAuthAsAgent()
        mockCreateUpdateDividendsIncomeValid()
        createUpdateDividendsIncomeController.createUpdateDividends(nino, taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe NO_CONTENT
    }

    "return an badRequest response when called as an individual with an invalid request body" in {
      val result = {
        mockAuth()
        createUpdateDividendsIncomeController.createUpdateDividends(nino, taxYear)(fakeGetRequest.withJsonBody(invalidJsonBody))
      }
      status(result) mustBe BAD_REQUEST
    }

    "return a badRequest response when called as an individual and IF returns badRequest" in {
      val result = {
        mockAuth()
        mockCreateUpdateDividendsIncomeBadRequest()
        createUpdateDividendsIncomeController.createUpdateDividends(nino, taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe BAD_REQUEST
    }

    "return an badRequest response when called as an agent and IF returns badRequest" in {
      val result = {
        mockAuthAsAgent()
        mockCreateUpdateDividendsIncomeBadRequest()
        createUpdateDividendsIncomeController.createUpdateDividends(nino, taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe BAD_REQUEST
    }

    "return a notFound response when called as an individual and IF returns badRequest" in {
      val result = {
        mockAuth()
        mockCreateUpdateDividendsIncomeNotFound()
        createUpdateDividendsIncomeController.createUpdateDividends(nino, taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe NOT_FOUND
    }

    "return an notFound response when called as an agent and IF returns badRequest" in {
      val result = {
        mockAuthAsAgent()
        mockCreateUpdateDividendsIncomeNotFound()
        createUpdateDividendsIncomeController.createUpdateDividends(nino, taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe NOT_FOUND
    }

    "return an InternalServerError response when called as an individual" in {
      val result = {
        mockAuth()
        mockCreateUpdateDividendsIncomeServerError()
        createUpdateDividendsIncomeController.createUpdateDividends(nino, taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return an InternalServerError response when called as an agent" in {
      val result = {
        mockAuthAsAgent()
        mockCreateUpdateDividendsIncomeServerError()
        createUpdateDividendsIncomeController.createUpdateDividends(nino, taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return an ServiceUnavailable response when called as an individual" in {
      val result = {
        mockAuth()
        mockCreateUpdateDividendsIncomeServiceUnavailable()
        createUpdateDividendsIncomeController.createUpdateDividends(nino, taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe SERVICE_UNAVAILABLE
    }

    "return an ServiceUnavailable response when called as an agent" in {
      val result = {
        mockAuthAsAgent()
        mockCreateUpdateDividendsIncomeServiceUnavailable()
        createUpdateDividendsIncomeController.createUpdateDividends(nino, taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe SERVICE_UNAVAILABLE
    }
  }
}
