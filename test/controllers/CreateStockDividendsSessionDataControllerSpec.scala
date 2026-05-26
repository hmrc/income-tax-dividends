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

import connectors.IncomeSourceConnector
import models.User
import models.dividends.StockDividendsCheckYourAnswersModel
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.NoContent
import play.api.test.FakeRequest
import repositories.StockDividendsUserDataRepository
import services.StockDividendsSessionService
import uk.gov.hmrc.http.SessionKeys
import utils.TestUtils

import scala.concurrent.{ExecutionContext, Future}

class CreateStockDividendsSessionDataControllerSpec extends TestUtils {

  private val mockStockDividendsUserDataRepository: StockDividendsUserDataRepository = mock[StockDividendsUserDataRepository]
  private val mockIncomeSourceConnector: IncomeSourceConnector = mock[IncomeSourceConnector]
  val stockDividendsSessionService: StockDividendsSessionService = new StockDividendsSessionService(mockStockDividendsUserDataRepository, mockIncomeSourceConnector) {
    override def createSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int)(onFail: => A)(onSuccess: => A)
                                     (implicit user: User[_], ec: ExecutionContext): Future[A] = Future.successful(NoContent.asInstanceOf[A])
  }
  val createStockDividendsSessionDataController = new CreateStockDividendsSessionDataController(stockDividendsSessionService,
    mockControllerComponents, authorisedAction)

  private val fakeGetRequest = FakeRequest("POST", "/").withHeaders("mtditid" -> mtditid, SessionKeys.sessionId -> "some-session-id")
  private val fakeGetRequestWithDifferentMTITID = FakeRequest("PUT", "/").withHeaders("mtditid" -> "123123123")

  val jsonBody: JsValue = Json.toJson(completeStockDividendsCYAModel)
  val invalidJsonBody: JsValue = Json.parse("""{"notukDividends": 12345.99, "nototherUkDividends": 123456.99}""")

  def mockUpdateDividendsSessionDataValid(): Unit = ()

  "calling .createSessionData" should {

    "return a 204 No Content response when called as an individual" in {
      val result = {
        mockAuth()
        mockUpdateDividendsSessionDataValid()
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
        mockUpdateDividendsSessionDataValid()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe NO_CONTENT
    }

    "return an badRequest response when called as an individual with an invalid request body" in {
      val result = {
        mockAuth()
        createStockDividendsSessionDataController.createSessionData(taxYear)(fakeGetRequest)
      }
      status(result) mustBe BAD_REQUEST
    }
  }
}
