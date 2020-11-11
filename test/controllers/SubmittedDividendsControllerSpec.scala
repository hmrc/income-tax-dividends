/*
 * Copyright 2020 HM Revenue & Customs
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

import connectors.httpParsers.SubmittedDividendsHttpParser.{SubmittedDividendsInvalidJsonException, SubmittedDividendsResponse}
import models.SubmittedDividendsModel
import org.scalamock.handlers.CallHandler3
import play.api.test.FakeRequest
import services.SubmittedDividendsService
import utils.TestUtils
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class SubmittedDividendsControllerSpec extends TestUtils {

  val submittedDividendsService: SubmittedDividendsService = mock[SubmittedDividendsService]
  val submittedDividendsController = new SubmittedDividendsController(submittedDividendsService, mockControllerComponents,authorisedAction)
  val nino :String = "123456789"
  val mtdItID :String = "123123123"
  val taxYear: Int = 1234
  private val fakeGetRequest = FakeRequest("GET", "/").withSession("MTDITID" -> "12234567890")

  def mockGetSubmittedDividendsValid(): CallHandler3[String, Int, HeaderCarrier, Future[SubmittedDividendsResponse]] = {
    val validSubmittedDividends: SubmittedDividendsResponse = Right(SubmittedDividendsModel(Some(12345.67),Some(12345.67)))
    (submittedDividendsService.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
      .expects(*, *, *)
      .returning(Future.successful(validSubmittedDividends))
  }

  def mockGetSubmittedDividendsInvalid(): CallHandler3[String, Int, HeaderCarrier, Future[SubmittedDividendsResponse]] = {
    val invalidSubmittedDividends: SubmittedDividendsResponse = Left(SubmittedDividendsInvalidJsonException)
    (submittedDividendsService.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
      .expects(*, *, *)
      .returning(Future.successful(invalidSubmittedDividends))
  }



  "calling .getSubmittedDividends" should {

    "with existing dividend sources" should {

      "return an OK 200 response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetSubmittedDividendsValid()
          submittedDividendsController.getSubmittedDividends(nino, taxYear, mtdItID)(fakeGetRequest)
        }
        status(result) mustBe OK
      }

      "return an OK 200 response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetSubmittedDividendsValid()
          submittedDividendsController.getSubmittedDividends(nino, taxYear, mtdItID)(fakeGetRequest)
        }
        status(result) mustBe OK
      }

    }
    "without existing dividend sources" should {

      "return an InternalServerError response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetSubmittedDividendsInvalid()
          submittedDividendsController.getSubmittedDividends(nino, taxYear, mtdItID)(fakeGetRequest)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "return an InternalServerError response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetSubmittedDividendsInvalid()
          submittedDividendsController.getSubmittedDividends(nino, taxYear, mtdItID)(fakeGetRequest)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

    }

  }
}
