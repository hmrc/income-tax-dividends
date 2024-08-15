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

import models.User
import models.dividends.StockDividendsCheckYourAnswersModel
import org.scalamock.handlers.CallHandler7
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.NoContent
import play.api.test.FakeRequest
import services.StockDividendsSessionService
import utils.TestUtils

import scala.concurrent.{ExecutionContext, Future}

class UpdateStockDividendsSessionDataControllerSpec extends TestUtils {

  val stockDividendsSessionService: StockDividendsSessionService = mock[StockDividendsSessionService]
  val updateDividendsSessionDataController =
    new UpdateStockDividendsSessionDataController(stockDividendsSessionService, mockControllerComponents, authorisedAction)

  private val fakeGetRequest = FakeRequest("PUT", "/").withHeaders("mtditid" -> mtditid)
  private val fakeGetRequestWithDifferentMTITID = FakeRequest("PUT", "/").withHeaders("mtditid" -> "123123123")

  val jsonBody: JsValue = Json.toJson(completeStockDividendsCYAModel)

  def mockUpdateDividendsSessionDataValid():
  CallHandler7[StockDividendsCheckYourAnswersModel, Int, Boolean, Result, Result, User[_], ExecutionContext, Future[Result]] = {
    val response: Result = NoContent
    (stockDividendsSessionService
      .updateSessionData(_: StockDividendsCheckYourAnswersModel, _: Int, _: Boolean)(_: Result)(_: Result)(_: User[_], _: ExecutionContext))
      .expects(*, *, *, *, *, *, *)
      .returning(Future.successful(response))
  }

  "calling .updateSessionData" should {

    "return a 204 No Content response when called as an individual" in {
      val result = {
        mockAuth()
        mockUpdateDividendsSessionDataValid()
        updateDividendsSessionDataController.updateSessionData(taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe NO_CONTENT
    }

    "return a 401 when called as an individual without correct mtditid" in {
      val result = {
        mockAuth()
        updateDividendsSessionDataController.updateSessionData(taxYear)(fakeGetRequestWithDifferentMTITID.withJsonBody(jsonBody))
      }
      status(result) mustBe UNAUTHORIZED
    }

    "return a 204 No Content response when called as an agent" in {
      val result = {
        mockAuthAsAgent()
        mockUpdateDividendsSessionDataValid()
        updateDividendsSessionDataController.updateSessionData(taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe NO_CONTENT
    }

    "return an badRequest response when called as an individual with an invalid request body" in {
      val result = {
        mockAuth()
        updateDividendsSessionDataController.updateSessionData(taxYear)(fakeGetRequest)
      }
      status(result) mustBe BAD_REQUEST
    }
  }
}
