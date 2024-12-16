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

package controllers

import models.User
import models.mongo.{DataNotFound, DatabaseError, StockDividendsUserDataModel}
import org.scalamock.handlers.CallHandler3
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout}
import services.StockDividendsSessionService
import uk.gov.hmrc.http.SessionKeys
import utils.TestUtils

import scala.concurrent.{ExecutionContext, Future}

class GetStockDividendsSessionDataControllerSpec extends TestUtils {

  private val dividendsSessionService: StockDividendsSessionService = mock[StockDividendsSessionService]
  private val getDividendsSessionDataController =
    new GetStockDividendsSessionDataController(dividendsSessionService, mockControllerComponents, authorisedAction)

  private val fakeGetRequest = FakeRequest("GET", "/").withHeaders("mtditid" -> mtditid, SessionKeys.sessionId -> "some-session-id")
  private val fakeGetRequestWithDifferentMTITID = FakeRequest("PUT", "/").withHeaders("mtditid" -> "123123123")

  val jsonBody: JsValue = Json.toJson(completeStockDividendsCYAModel)

  val responseWithData: Right[Nothing, Some[StockDividendsUserDataModel]] =
    Right(Some(StockDividendsUserDataModel("sessionId", mtditid, nino, taxYear, Some(completeStockDividendsCYAModel))))

  val responseWithoutData: Right[Nothing, Option[StockDividendsUserDataModel]] =
    Right(None)

  def mockGetSessionDataSuccess():
  CallHandler3[Int, User[_], ExecutionContext, Future[Either[DatabaseError, Option[StockDividendsUserDataModel]]]] = {
    val response = responseWithData
    (dividendsSessionService
      .getSessionData( _: Int)(_: User[_], _: ExecutionContext))
      .expects(*, *, *)
      .returning(Future.successful(response))
  }

  def mockGetSessionDataNotFound():
  CallHandler3[Int, User[_], ExecutionContext, Future[Either[DatabaseError, Option[StockDividendsUserDataModel]]]] = {
    val response = Left(DataNotFound)
    (dividendsSessionService
      .getSessionData( _: Int)(_: User[_], _: ExecutionContext))
      .expects(*, *, *)
      .returning(Future.successful(response))
  }

  def mockGetSessionDataEmptyData():
  CallHandler3[Int, User[_], ExecutionContext, Future[Either[DatabaseError, Option[StockDividendsUserDataModel]]]] = {
    val response = responseWithoutData
    (dividendsSessionService
      .getSessionData( _: Int)(_: User[_], _: ExecutionContext))
      .expects(*, *, *)
      .returning(Future.successful(response))
  }

  "calling .getSessionData" should {

    "return a 200 response when called as an individual" in {
      val result = {
        mockAuth()
        mockGetSessionDataSuccess()
        getDividendsSessionDataController.getSessionData(taxYear)(fakeGetRequest)
      }
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(responseWithData.value)
    }

    "return a 401 when called as an individual" in {
      val result = {
        mockAuth()
        getDividendsSessionDataController.getSessionData(taxYear)(fakeGetRequestWithDifferentMTITID)
      }
      status(result) mustBe UNAUTHORIZED
    }

    "return a 200 response when called as an agent" in {
      val result = {
        mockAuthAsAgent()
        mockGetSessionDataSuccess()
        getDividendsSessionDataController.getSessionData(taxYear)(fakeGetRequest)
      }
      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(responseWithData.value)
    }

    "return 404 when data does not exist" in {
      val result = {
        mockAuth()
        mockGetSessionDataNotFound()
        getDividendsSessionDataController.getSessionData(taxYear)(fakeGetRequest)
      }
      status(result) mustBe NOT_FOUND
    }

    "return 404 when None is returned" in {
      val result = {
        mockAuth()
        mockGetSessionDataEmptyData()
        getDividendsSessionDataController.getSessionData(taxYear)(fakeGetRequest)
      }
      status(result) mustBe NOT_FOUND
    }
  }
}
