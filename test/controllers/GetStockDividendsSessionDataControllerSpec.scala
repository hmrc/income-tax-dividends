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

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.when

import models.User
import models.mongo.{DataNotFound, DatabaseError, StockDividendsUserDataModel}
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
  Unit = {
    val response = responseWithData
    when(dividendsSessionService
      .getSessionData( any[Int]())(any[User[_]](), any[ExecutionContext]())).thenReturn(Future.successful(response))
  }

  def mockGetSessionDataNotFound():
  Unit = {
    val response = Left(DataNotFound)
    when(dividendsSessionService
      .getSessionData( any[Int]())(any[User[_]](), any[ExecutionContext]())).thenReturn(Future.successful(response))
  }

  def mockGetSessionDataEmptyData():
  Unit = {
    val response = responseWithoutData
    when(dividendsSessionService
      .getSessionData( any[Int]())(any[User[_]](), any[ExecutionContext]())).thenReturn(Future.successful(response))
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
