/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.httpParsers.CreateOrAmendDividendsHttpParser.{CreateOrAmendDividendsResponse, CreateOrAmendDividendsServiceUnavailableException}
import models.CreateOrAmendDividendsModel
import org.scalamock.handlers.CallHandler4
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import services.CreateOrAmendDividendsService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class CreateOrAmendDividendsControllerSpec extends TestUtils {

  val createOrAmendDividendsService: CreateOrAmendDividendsService = mock[CreateOrAmendDividendsService]
  val createOrAmendDividendsController = new CreateOrAmendDividendsController(createOrAmendDividendsService, mockControllerComponents, authorisedAction)
  val nino: String = "123456789"
  val mtdItID: String = "123123123"
  val taxYear: Int = 1234
  private val fakeGetRequest = FakeRequest("PUT", "/").withSession("MTDITID" -> "12234567890")

  val jsonBody: JsValue = Json.parse("""{"ukDividends": 12345.99, "otherUkDividends": 123456.99}""")
  val invalidJsonBody: JsValue = Json.parse("""{"notukDividends": 12345.99, "nototherUkDividends": 123456.99}""")

  def mockCreateOrAmendDividendsValid(): CallHandler4[String, Int, CreateOrAmendDividendsModel, HeaderCarrier, Future[CreateOrAmendDividendsResponse]] = {
    val response: CreateOrAmendDividendsResponse = Right(true)
    (createOrAmendDividendsService.createOrAmendDividends(_: String, _: Int, _: CreateOrAmendDividendsModel)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }

  def mockGetSubmittedDividendsInvalid(): CallHandler4[String, Int, CreateOrAmendDividendsModel, HeaderCarrier, Future[CreateOrAmendDividendsResponse]] = {
    val response: CreateOrAmendDividendsResponse = Left(CreateOrAmendDividendsServiceUnavailableException)
    (createOrAmendDividendsService.createOrAmendDividends(_: String, _: Int, _: CreateOrAmendDividendsModel)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }


  "calling .createOrAmendDividends" should {

    "return a 204 No Content response when called as an individual" in {
      val result = {
        mockAuth()
        mockCreateOrAmendDividendsValid()
        createOrAmendDividendsController.createOrAmendDividends(nino, taxYear, mtdItID)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe NO_CONTENT
    }

    "return an OK 200 response when called as an agent" in {
      val result = {
        mockAuthAsAgent()
        mockCreateOrAmendDividendsValid()
        createOrAmendDividendsController.createOrAmendDividends(nino, taxYear, mtdItID)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe NO_CONTENT
    }

    "return an badRequest response when called as an individual" in {
      val result = {
        mockAuth()
        createOrAmendDividendsController.createOrAmendDividends(nino, taxYear, mtdItID)(fakeGetRequest.withJsonBody(invalidJsonBody))
      }
      status(result) mustBe BAD_REQUEST
    }

    "return an InternalServerError response when called as an individual" in {
      val result = {
        mockAuth()
        mockGetSubmittedDividendsInvalid()
        createOrAmendDividendsController.createOrAmendDividends(nino, taxYear, mtdItID)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe INTERNAL_SERVER_ERROR
    }

    "return an InternalServerError response when called as an agent" in {
      val result = {
        mockAuthAsAgent()
        mockGetSubmittedDividendsInvalid()
        createOrAmendDividendsController.createOrAmendDividends(nino, taxYear, mtdItID)(fakeGetRequest.withJsonBody(jsonBody))
      }
      status(result) mustBe INTERNAL_SERVER_ERROR
    }


  }
}
