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

import connectors.httpParsers.GetDividendsIncomeParser.GetDividendsIncomeDataResponse
import models._
import org.scalamock.handlers.CallHandler3
import play.api.http.Status._
import play.api.libs.json.Json
import services.GetDividendsIncomeService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class GetDividendsIncomeControllerSpec extends TestUtils {

  val serviceMock: GetDividendsIncomeService = mock[GetDividendsIncomeService]
  val controller = new GetDividendsIncomeController(serviceMock, mockControllerComponents, authorisedAction)

  val notFoundModel: ErrorModel = ErrorModel(NOT_FOUND, ErrorBodyModel("NotFound", "Unable to find source"))
  val serviceUnavailableModel: ErrorModel =
    ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("SERVICE_UNAVAILABLE", "The service is currently unavailable"))
  val badRequestModel: ErrorModel = ErrorModel(BAD_REQUEST, ErrorBodyModel("BAD_REQUEST", "The supplied NINO is invalid"))
  val internalServerErrorModel: ErrorModel =
    ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("INTERNAL_SERVER_ERROR", "There has been an unexpected error"))

  val dividendsIncomeDataModel: DividendsIncomeDataModel = DividendsIncomeDataModel(
    submittedOn = Some("2020-06-17T10:53:38Z"),
    foreignDividend = Some(Seq(ForeignDividendModel("BES", Some(2323.56), Some(5435.56), Some(4564.67), Some(true), 4564.67))),
    dividendIncomeReceivedWhilstAbroad = Some(Seq(ForeignDividendModel("CHN", Some(5664.67),
      Some(5657.56), Some(4644.56), Some(true), 4654.56))),
    stockDividend = Some(StockDividendModel(Some("Stock Dividend Customer Reference"), 2525.89)),
    redeemableShares = Some(StockDividendModel(Some("Redeemable Shares Customer Reference"), 3535.56)),
    bonusIssuesOfSecurities = Some(StockDividendModel(Some("Bonus Issues Of Securities Customer Reference"), 5633.67)),
    closeCompanyLoansWrittenOff = Some(StockDividendModel(Some("Close Company Loans WrittenOff Customer Reference"), 6743.23))
  )

  ".getDividendsIncomeData" should {

    "Return a 200 OK response with valid DividendsIncomeData" in {

      val serviceResult = Right(dividendsIncomeDataModel)
      val finalResult = Json.toJson(dividendsIncomeDataModel).toString()

      def serviceCallMock(): CallHandler3[String, Int, HeaderCarrier, Future[GetDividendsIncomeDataResponse]] =
        (serviceMock.getDividendsIncomeData(_: String, _: Int)(_: HeaderCarrier))
          .expects(nino, taxYear, *)
          .returning(Future.successful(serviceResult))


      val result = {
        mockAuth()
        serviceCallMock()
        controller.getDividendsIncomeData(nino, taxYear)(fakeRequest)
      }

      status(result) mustBe OK
      bodyOf(result) mustBe finalResult
    }

    "return a Left response" when {

      def mockGetDividendsIncomeDataWithError(errorModel: ErrorModel): CallHandler3[String, Int, HeaderCarrier, Future[GetDividendsIncomeDataResponse]] = {
        (serviceMock.getDividendsIncomeData(_: String, _: Int)(_: HeaderCarrier))
          .expects(nino, taxYear, *)
          .returning(Future.successful(Left(errorModel)))
      }

      "the service returns a NO_CONTENT" in {
        val result = {
          mockAuth()
          mockGetDividendsIncomeDataWithError(notFoundModel)
          controller.getDividendsIncomeData(nino, taxYear)(fakeRequest)
        }
        status(result) mustBe NOT_FOUND
      }

      "the service returns a SERVICE_UNAVAILABLE" in {
        val result = {
          mockAuth()
          mockGetDividendsIncomeDataWithError(serviceUnavailableModel)
          controller.getDividendsIncomeData(nino, taxYear)(fakeRequest)
        }
        status(result) mustBe SERVICE_UNAVAILABLE
      }

      "the service returns a BAD_REQUEST" in {
        val result = {
          mockAuth()
          mockGetDividendsIncomeDataWithError(badRequestModel)
          controller.getDividendsIncomeData(nino, taxYear)(fakeRequest)
        }
        status(result) mustBe BAD_REQUEST
      }

      "the service returns a INTERNAL_SERVER_ERROR" in {
        val result = {
          mockAuth()
          mockGetDividendsIncomeDataWithError(internalServerErrorModel)
          controller.getDividendsIncomeData(nino, taxYear)(fakeRequest)
        }
        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
