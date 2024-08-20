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
import models.taskList._
import models._
import play.api.http.Status.NOT_FOUND
import support.providers.AppConfigStubProvider
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class CommonTaskListServiceSpec extends TestUtils with AppConfigStubProvider {

  val dividendsService: SubmittedDividendsService = mock[SubmittedDividendsService]
  val stockDividendsService: GetDividendsIncomeService = mock[GetDividendsIncomeService]

  val service: CommonTaskListService = new CommonTaskListService(appConfigStub, dividendsService, stockDividendsService)

  val nino: String = "12345678"
  val taxYear: Int = 1234

  val fullDividendsResult: SubmittedDividendsResponse = Right(SubmittedDividendsModel(Some(20.00), Some(20.00), None))
  val emptyDividendsResult: SubmittedDividendsResponse = Left(ErrorModel(NOT_FOUND, ErrorBodyModel("SOME_CODE", "reason")))

  val fullStockDividendsResult: GetDividendsIncomeDataResponse = Right(DividendsIncomeDataModel(
    submittedOn = Some("2020-06-17T10:53:38Z"),
    foreignDividend = None,
    dividendIncomeReceivedWhilstAbroad = None,
    stockDividend = Some(StockDividendModel(Some("Stock Dividend Customer Reference"), 2525.89)),
    redeemableShares = Some(StockDividendModel(Some("Redeemable Shares Customer Reference"), 3535.56)),
    bonusIssuesOfSecurities = None,
    closeCompanyLoansWrittenOff = Some(StockDividendModel(Some("Close Company Loans WrittenOff Customer Reference"), 6743.23))
  ))
  val emptyStockDividendsResult: GetDividendsIncomeDataResponse = Left(ErrorModel(NOT_FOUND, ErrorBodyModel("SOME_CODE", "reason")))

  val fullTaskSection: TaskListSection =
    TaskListSection(SectionTitle.DividendsTitle,
      Some(List(
        TaskListSectionItem(TaskTitle.CashDividends, TaskStatus.Completed,
          Some("http://localhost:9308/update-and-submit-income-tax-return/personal-income/1234/dividends/how-much-dividends-from-uk-companies")),
        TaskListSectionItem(TaskTitle.DividendsFromUnitTrusts, TaskStatus.Completed,
          Some("http://localhost:9308/update-and-submit-income-tax-return/personal-income/" +
            "1234/dividends/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies")),
        TaskListSectionItem(TaskTitle.StockDividends, TaskStatus.Completed,
          Some("http://localhost:9308/update-and-submit-income-tax-return/personal-income/1234/dividends/stock-dividend-amount")),
        TaskListSectionItem(TaskTitle.FreeRedeemableShares, TaskStatus.Completed,
          Some("http://localhost:9308/update-and-submit-income-tax-return/personal-income/1234/dividends/redeemable-shares-amount")),
        TaskListSectionItem(TaskTitle.CloseCompanyLoans, TaskStatus.Completed,
          Some("http://localhost:9308/update-and-submit-income-tax-return/personal-income/1234/dividends/close-company-loan-amount"))
      ))
    )

  "CommonTaskListService.get" should {

    "return a full task list section model" in {

      (dividendsService.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(fullDividendsResult))

      (stockDividendsService.getDividendsIncomeData(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(fullStockDividendsResult))

      val underTest = service.get(taxYear, nino)

      await(underTest) mustBe fullTaskSection
    }

    "return a minimal task list section model" in {

      (dividendsService.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(Right(SubmittedDividendsModel(Some(20.00), None, None))))

      (stockDividendsService.getDividendsIncomeData(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(emptyStockDividendsResult))

      val underTest = service.get(taxYear, nino)

      await(underTest) mustBe fullTaskSection.copy(
        taskItems = Some(List(
          TaskListSectionItem(TaskTitle.CashDividends, TaskStatus.Completed, Some("http://localhost:9308/update-and-submit-income-tax-return/personal-income/1234/dividends/how-much-dividends-from-uk-companies"))
        ))
      )
    }

    "return an empty task list section model" in {

      (dividendsService.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(emptyDividendsResult))

      (stockDividendsService.getDividendsIncomeData(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(emptyStockDividendsResult))

      val underTest = service.get(taxYear, nino)

      await(underTest) mustBe TaskListSection(SectionTitle.DividendsTitle, None)
    }
  }
}
