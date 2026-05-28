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

import connectors.httpParsers.GetAnnualIncomeSourcePeriodHttpParser.GetAnnualIncomeSourcePeriod
import models.{ErrorBodyModel, ErrorModel, SubmittedDividendsModel}
import play.api.http.Status._
import play.api.test.FakeRequest
import services.SubmittedDividendsService
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import utils.{TaxYearUtils, TestUtils}

import scala.concurrent.Future

class SubmittedDividendsIfControllerSpec extends TestUtils {

  val submittedDividendsService: SubmittedDividendsService = mock[SubmittedDividendsService]
  val submittedDividendsController = new SubmittedDividendsController(submittedDividendsService, mockControllerComponents,authorisedAction)

  val mtdItID :String = "1234567890"
  val specificTaxYear: Int = TaxYearUtils.specificTaxYear
  val badRequestModel: ErrorBodyModel = ErrorBodyModel("INVALID_NINO", "Nino is invalid")
  val notFoundModel: ErrorBodyModel = ErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source")
  val serverErrorModel: ErrorBodyModel = ErrorBodyModel("SERVER_ERROR", "Internal server error")
  val serviceUnavailableErrorModel: ErrorBodyModel = ErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable")
  val unprocessableEntityErrorModel: ErrorBodyModel = ErrorBodyModel("UNPROCESSABLE_ENTITY", "Tax Year unsupported")
  private val fakeGetRequest = FakeRequest("GET", "/").withHeaders("mtditid" -> mtdItID, SessionKeys.sessionId -> "some-session-id")
  private val fakeGetRequestWithDifferentMTDITID = FakeRequest("GET", "/").withHeaders("mtditid" -> "123123123")

  def mockGetSubmittedDividendsValid(): Unit = {
    val validSubmittedDividends: GetAnnualIncomeSourcePeriod = Right(SubmittedDividendsModel(Some(12345.67),Some(12345.67)))
    when(submittedDividendsService.getSubmittedDividends(any[String](), any[Int]())(any[HeaderCarrier]())).thenReturn(Future.successful(validSubmittedDividends))
  }

  def mockGetSubmittedDividendsBadRequest(): Unit = {
    val invalidSubmittedDividends: GetAnnualIncomeSourcePeriod = Left(ErrorModel(BAD_REQUEST, badRequestModel))
    when(submittedDividendsService.getSubmittedDividends(any[String](), any[Int]())(any[HeaderCarrier]())).thenReturn(Future.successful(invalidSubmittedDividends))
  }

  def mockGetSubmittedDividendsNotFound(): Unit = {
    val invalidSubmittedDividends: GetAnnualIncomeSourcePeriod = Left(ErrorModel(NOT_FOUND, notFoundModel))
    when(submittedDividendsService.getSubmittedDividends(any[String](), any[Int]())(any[HeaderCarrier]())).thenReturn(Future.successful(invalidSubmittedDividends))
  }

  def mockGetSubmittedDividendsServerError(): Unit = {
    val invalidSubmittedDividends: GetAnnualIncomeSourcePeriod = Left(ErrorModel(INTERNAL_SERVER_ERROR, serverErrorModel))
    when(submittedDividendsService.getSubmittedDividends(any[String](), any[Int]())(any[HeaderCarrier]())).thenReturn(Future.successful(invalidSubmittedDividends))
  }

  def mockGetSubmittedDividendsServiceUnavailable(): Unit = {
    val invalidSubmittedDividends: GetAnnualIncomeSourcePeriod = Left(ErrorModel(SERVICE_UNAVAILABLE, serviceUnavailableErrorModel))
    when(submittedDividendsService.getSubmittedDividends(any[String](), any[Int]())(any[HeaderCarrier]())).thenReturn(Future.successful(invalidSubmittedDividends))
  }

  def mockGetSubmittedDividendsUnprocessableEntity(): Unit = {
    val invalidSubmittedDividends: GetAnnualIncomeSourcePeriod = Left(ErrorModel(UNPROCESSABLE_ENTITY, unprocessableEntityErrorModel))
    when(submittedDividendsService.getSubmittedDividends(any[String](), any[Int]())(any[HeaderCarrier]())).thenReturn(Future.successful(invalidSubmittedDividends))
  }

  "calling .getSubmittedDividends" should {

    "with existing dividend sources" should {

      "return an OK 200 response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetSubmittedDividendsValid()
          submittedDividendsController.getSubmittedDividends(nino, specificTaxYear)(fakeGetRequest)
        }
        status(result) mustBe OK
      }

      "return an 401 response when called as an individual" in {
        val result = {
          mockAuth()
          submittedDividendsController.getSubmittedDividends(nino, specificTaxYear)(fakeGetRequestWithDifferentMTDITID)
        }
        status(result) mustBe UNAUTHORIZED
      }

      "return an OK 200 response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetSubmittedDividendsValid()
          submittedDividendsController.getSubmittedDividends(nino, specificTaxYear)(fakeGetRequest)
        }
        status(result) mustBe OK
      }

    }

    "without existing dividend sources" should {

      "return an NotFound response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetSubmittedDividendsNotFound()
          submittedDividendsController.getSubmittedDividends(nino, specificTaxYear)(fakeGetRequest)
        }
        status(result) mustBe NOT_FOUND
      }

      "return an NotFound response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetSubmittedDividendsNotFound()
          submittedDividendsController.getSubmittedDividends(nino, specificTaxYear)(fakeGetRequest)
        }
        status(result) mustBe NOT_FOUND
      }

    }

    "with an invalid NINO" should {

      "return an BadRequest response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetSubmittedDividendsBadRequest()
          submittedDividendsController.getSubmittedDividends(nino, specificTaxYear)(fakeGetRequest)
        }
        status(result) mustBe BAD_REQUEST
      }

      "return an BadRequest response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetSubmittedDividendsBadRequest()
          submittedDividendsController.getSubmittedDividends(nino, specificTaxYear)(fakeGetRequest)
        }
        status(result) mustBe BAD_REQUEST
      }
    }

    "with something that causes and internal server error in DES" should {

      "return an BadRequest response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetSubmittedDividendsServerError()
          submittedDividendsController.getSubmittedDividends(nino, specificTaxYear)(fakeGetRequest)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }

      "return an BadRequest response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetSubmittedDividendsServerError()
          submittedDividendsController.getSubmittedDividends(nino, specificTaxYear)(fakeGetRequest)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }

    "with an unavailable service" should {

      "return an Service_Unavailable response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetSubmittedDividendsServiceUnavailable()
          submittedDividendsController.getSubmittedDividends(nino, specificTaxYear)(fakeGetRequest)
        }
        status(result) mustBe SERVICE_UNAVAILABLE
      }

      "return an Service_Unavailable response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetSubmittedDividendsServiceUnavailable()
          submittedDividendsController.getSubmittedDividends(nino, specificTaxYear)(fakeGetRequest)
        }
        status(result) mustBe SERVICE_UNAVAILABLE
      }
    }

    "with an unprocessable entity service" should {

      "return an Unprocessable_entity response when called as an individual" in {
        val result = {
          mockAuth()
          mockGetSubmittedDividendsUnprocessableEntity()
          submittedDividendsController.getSubmittedDividends(nino, specificTaxYear)(fakeGetRequest)
        }
        status(result) mustBe UNPROCESSABLE_ENTITY
      }

      "return an Unprocessable_entity response when called as an agent" in {
        val result = {
          mockAuthAsAgent()
          mockGetSubmittedDividendsUnprocessableEntity()
          submittedDividendsController.getSubmittedDividends(nino, specificTaxYear)(fakeGetRequest)
        }
        status(result) mustBe UNPROCESSABLE_ENTITY
      }
    }
  }
}
