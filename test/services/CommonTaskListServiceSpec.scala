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

package services

import connectors.httpParsers.GetDividendsIncomeParser.GetDividendsIncomeDataResponse
import connectors.httpParsers.SubmittedDividendsHttpParser.SubmittedDividendsResponse
import models.{DividendsIncomeDataModel, StockDividendModel, SubmittedDividendsModel}
import org.apache.pekko.stream.Materializer
import play.api.http.ContentTypes
import play.api.http.Status.OK
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class CommonTaskListServiceSpec extends TestUtils {

  val dividendsService: SubmittedDividendsService = mock[SubmittedDividendsService]
  val stockDividendsService: GetDividendsIncomeService = mock[GetDividendsIncomeService]

  val service: CommonTaskListService = new CommonTaskListService(mockAppConfig, dividendsService, stockDividendsService)

  val nino: String = "12345678"
  val taxYear: Int = 1234

  val fullDividendsResult: SubmittedDividendsResponse = Right(SubmittedDividendsModel(Some(20.00), Some(20.00), None))

  val fullStockDividendsResult: GetDividendsIncomeDataResponse = Right(DividendsIncomeDataModel(
    submittedOn = Some("2020-06-17T10:53:38Z"),
    foreignDividend = None,
    dividendIncomeReceivedWhilstAbroad = None,
    stockDividend = Some(StockDividendModel(Some("Stock Dividend Customer Reference"), 2525.89)),
    redeemableShares = Some(StockDividendModel(Some("Redeemable Shares Customer Reference"), 3535.56)),
    bonusIssuesOfSecurities = None,
    closeCompanyLoansWrittenOff = Some(StockDividendModel(Some("Close Company Loans WrittenOff Customer Reference"), 6743.23))
  ))

  "CommonTaskListService.get" should {

    "return a full task list section model" in {
      (dividendsService.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(fullDividendsResult))

      (stockDividendsService.getDividendsIncomeData(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(fullStockDividendsResult))

      val underTest = service.get(taxYear, nino)

      status(underTest) mustBe OK
      await(underTest).body mustBe ""
    }

    "return a minimal task list section model" in {

    }

    "return an empty task list section model" in {

    }
  }
}
