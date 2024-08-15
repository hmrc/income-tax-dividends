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
import org.scalamock.handlers.CallHandler6
import play.api.http.Status._
import play.api.mvc.Result
import play.api.mvc.Results.NoContent
import play.api.test.FakeRequest
import services.StockDividendsSessionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.{ExecutionContext, Future}

class DeleteStockDividendsSessionDataControllerSpec extends TestUtils {

  private val dividendsSessionService: StockDividendsSessionService = mock[StockDividendsSessionService]
  private val controller =
    new DeleteStockDividendsSessionDataController(dividendsSessionService, mockControllerComponents, authorisedAction)

  private val fakeGetRequest = FakeRequest("DELETE", "/").withHeaders("mtditid" -> mtditid)
  private val fakeGetRequestWithDifferentMTITID = FakeRequest("DELETE", "/").withHeaders("mtditid" -> "123123123")

  def mockDeleteDividendsSessionData():
  CallHandler6[Int, Result, Result, User[_], ExecutionContext, HeaderCarrier, Future[Result]] = {
    val response: Result = NoContent
    (dividendsSessionService
      .clear(_: Int)(_: Result)(_: Result)(_: User[_], _: ExecutionContext, _: HeaderCarrier))
      .expects(*, *, *, *, *, *)
      .returning(Future.successful(response))
  }

  "calling .clear" should {

    "return a 204 No Content response when called as an individual" in {
      val result = {
        mockAuth()
        mockDeleteDividendsSessionData()
        controller.clear(taxYear)(fakeGetRequest)
      }
      status(result) mustBe NO_CONTENT
    }

    "return a 401 when called as an individual" in {
      val result = {
        mockAuth()
        controller.clear(taxYear)(fakeGetRequestWithDifferentMTITID)
      }
      status(result) mustBe UNAUTHORIZED
    }

    "return a 204 No Content response when called as an agent" in {
      val result = {
        mockAuthAsAgent()
        mockDeleteDividendsSessionData()
        controller.clear(taxYear)(fakeGetRequest)
      }
      status(result) mustBe NO_CONTENT
    }
  }
}
