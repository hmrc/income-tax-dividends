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
import models._
import models.mongo.JourneyAnswers
import models.taskList.TaskStatus.{Completed, NotStarted}
import models.taskList.TaskTitle.{CashDividends, CloseCompanyLoans, DividendsFromUnitTrusts, FreeRedeemableShares, StockDividends}
import models.taskList.{TaskListSectionItem, _}
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.{JsObject, JsString, Json}
import support.mocks.{MockGetDividendsIncomeService, MockJourneyAnswersRepository, MockSubmittedDividendsService}
import support.providers.AppConfigStubProvider
import utils.TestUtils

import java.time.Instant

class CommonTaskListServiceSpec extends TestUtils
  with AppConfigStubProvider
  with MockJourneyAnswersRepository
  with MockGetDividendsIncomeService
  with MockSubmittedDividendsService {

  val service: CommonTaskListService = new CommonTaskListService(
    appConfig = appConfigStub,
    dividendsService = mockSubmittedDividendsService,
    stockDividendsService = mockGetDividendsIncomeService,
    journeyAnswersRepository = mockJourneyAnswersRepo
  )

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
          Some(s"http://localhost:9308/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/check-how-much-dividends-from-uk-companies")),
        TaskListSectionItem(TaskTitle.DividendsFromUnitTrusts, TaskStatus.Completed,
          Some("http://localhost:9308/update-and-submit-income-tax-return/personal-income/" +
            s"$taxYear/dividends/check-how-much-dividends-from-uk-trusts-and-open-ended-investment-companies")),
        TaskListSectionItem(TaskTitle.StockDividends, TaskStatus.Completed,
          Some(s"http://localhost:9308/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/check-stock-dividend-amount")),
        TaskListSectionItem(TaskTitle.FreeRedeemableShares, TaskStatus.Completed,
          Some(s"http://localhost:9308/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/check-redeemable-shares-amount")),
        TaskListSectionItem(TaskTitle.CloseCompanyLoans, TaskStatus.Completed,
          Some(s"http://localhost:9308/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/check-close-company-loan-amount"))
      ))
    )

  "CommonTaskList.get" should {
    val dummyException: RuntimeException = new RuntimeException("Dummy error")

    val baseUrl = "http://localhost:9308/update-and-submit-income-tax-return/personal-income"
    val ukDividendsUrl = s"$baseUrl/$taxYear/dividends/check-how-much-dividends-from-uk-companies"
    val otherUkDividendsUrl = s"$baseUrl/$taxYear/dividends/check-how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"
    val stockDividendsUrl = s"$baseUrl/$taxYear/dividends/check-stock-dividend-amount"
    val redeemableSharesUrl = s"$baseUrl/$taxYear/dividends/check-redeemable-shares-amount"
    val closeCompanyUrl = s"$baseUrl/$taxYear/dividends/check-close-company-loan-amount"

    "when an exception occurs" must {
      "handle appropriately when failing to retrieve ukJourneyAnswers" in {
        mockGetJourneyAnswersException(mtditid, taxYear, "cash-dividends", dummyException)
        def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
        assertThrows[RuntimeException](result)
      }

      "handle appropriately when failing to retrieve otherJourneyAnswers" in {
        mockGetJourneyAnswers(mtditid, taxYear, "cash-dividends", None)
        mockGetJourneyAnswersException(mtditid, taxYear, "dividends-from-unit-trusts", dummyException)
        def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
        assertThrows[RuntimeException](result)
      }
      
      "handle appropriately when failing to retrieve data from SubmittedDividendsService" in {
        mockGetJourneyAnswers(mtditid, taxYear, "cash-dividends", None)
        mockGetJourneyAnswers(mtditid, taxYear, "dividends-from-unit-trusts", None)
        mockGetSubmittedDividendsFailure(nino, taxYear, dummyException)
        def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
        assertThrows[RuntimeException](result)
      }

      "handle appropriately when failing to retrieve stockJourneyAnswers" in {
        mockGetJourneyAnswers(mtditid, taxYear, "cash-dividends", None)
        mockGetJourneyAnswers(mtditid, taxYear, "dividends-from-unit-trusts", None)
        mockGetSubmittedDividendsSuccess(nino, taxYear, fullDividendsResult)

        mockGetJourneyAnswersException(mtditid, taxYear, "stock-dividends", dummyException)

        def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
        assertThrows[RuntimeException](result)
      }

      "handle appropriately when failing to retrieve redeemableJourneyAnswers" in {
        mockGetJourneyAnswers(mtditid, taxYear, "cash-dividends", None)
        mockGetJourneyAnswers(mtditid, taxYear, "dividends-from-unit-trusts", None)
        mockGetSubmittedDividendsSuccess(nino, taxYear, fullDividendsResult)
        
        mockGetJourneyAnswers(mtditid, taxYear, "stock-dividends", None)
        mockGetJourneyAnswersException(mtditid, taxYear, "free-redeemable-shares", dummyException)

        def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
        assertThrows[RuntimeException](result)
      }
      
      "handle appropriately when failing to retrieve closeCompanyJourneyAnswers" in {
        mockGetJourneyAnswers(mtditid, taxYear, "cash-dividends", None)
        mockGetJourneyAnswers(mtditid, taxYear, "dividends-from-unit-trusts", None)
        mockGetSubmittedDividendsSuccess(nino, taxYear, fullDividendsResult)

        mockGetJourneyAnswers(mtditid, taxYear, "stock-dividends", None)
        mockGetJourneyAnswers(mtditid, taxYear, "free-redeemable-shares", None)
        mockGetJourneyAnswersException(mtditid, taxYear, "close-company-loans", dummyException)

        def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
        assertThrows[RuntimeException](result)
      }

      "handle appropriately when failing to retrieve data from GetDividendsIncomeService" in {
        mockGetJourneyAnswers(mtditid, taxYear, "cash-dividends", None)
        mockGetJourneyAnswers(mtditid, taxYear, "dividends-from-unit-trusts", None)
        mockGetSubmittedDividendsSuccess(nino, taxYear, fullDividendsResult)

        mockGetJourneyAnswers(mtditid, taxYear, "stock-dividends", None)
        mockGetJourneyAnswers(mtditid, taxYear, "free-redeemable-shares", None)
        mockGetJourneyAnswers(mtditid, taxYear, "close-company-loans", None)
        mockGetDividendsIncomeDataFailure(nino, taxYear, dummyException)

        def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
        assertThrows[RuntimeException](result)
      }
    }

    "when an error occurs while retrieving user data from DES for both services" should {
      "return an empty task list" in {
        mockGetJourneyAnswers(mtditid, taxYear, "cash-dividends", None)
        mockGetJourneyAnswers(mtditid, taxYear, "dividends-from-unit-trusts", None)
        mockGetSubmittedDividendsSuccess(nino, taxYear, emptyDividendsResult)

        mockGetJourneyAnswers(mtditid, taxYear, "stock-dividends", None)
        mockGetJourneyAnswers(mtditid, taxYear, "free-redeemable-shares", None)
        mockGetJourneyAnswers(mtditid, taxYear, "close-company-loans", None)
        mockGetDividendsIncomeDataSuccess(nino, taxYear, emptyStockDividendsResult)

        def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
        result mustBe TaskListSection(SectionTitle.DividendsTitle, None)
      }
    }

    "when an error occurs while retrieving user data from DES for one service" should {
      "return the expected result" in {
        mockGetJourneyAnswers(mtditid, taxYear, "cash-dividends", None)
        mockGetJourneyAnswers(mtditid, taxYear, "dividends-from-unit-trusts", None)
        mockGetSubmittedDividendsSuccess(nino, taxYear, fullDividendsResult)

        mockGetJourneyAnswers(mtditid, taxYear, "stock-dividends", None)
        mockGetJourneyAnswers(mtditid, taxYear, "free-redeemable-shares", None)
        mockGetJourneyAnswers(mtditid, taxYear, "close-company-loans", None)
        mockGetDividendsIncomeDataSuccess(nino, taxYear, emptyStockDividendsResult)

        val tasks = Seq(
          TaskListSectionItem(CashDividends, Completed, Some(ukDividendsUrl)),
          TaskListSectionItem(DividendsFromUnitTrusts, Completed, Some(otherUkDividendsUrl))
        )

        def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
        result mustBe TaskListSection(SectionTitle.DividendsTitle, Some(tasks))
      }
    }

    "when journey answers are defined" should {
      "return an error when the status field is missing" in {
        val journeyAnswers = JourneyAnswers(mtditid, taxYear, "cash-dividends", JsObject.empty, Instant.now())
        mockGetJourneyAnswers(mtditid, taxYear, "cash-dividends", Some(journeyAnswers))
        mockGetJourneyAnswers(mtditid, taxYear, "dividends-from-unit-trusts", None)
        mockGetSubmittedDividendsSuccess(nino, taxYear, fullDividendsResult)

        def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
        assertThrows[NoSuchElementException](result)
      }

      "return 'NotStarted' status when the status field can't be parsed" in {
        val journeyAnswers = JourneyAnswers(mtditid, taxYear, "cash-dividends", Json.obj("status" -> JsString("nonsense")), Instant.now())
        mockGetJourneyAnswers(mtditid, taxYear, "cash-dividends", Some(journeyAnswers))
        mockGetJourneyAnswers(mtditid, taxYear, "dividends-from-unit-trusts", None)
        mockGetSubmittedDividendsSuccess(nino, taxYear, fullDividendsResult)

        mockGetJourneyAnswers(mtditid, taxYear, "stock-dividends", None)
        mockGetJourneyAnswers(mtditid, taxYear, "free-redeemable-shares", None)
        mockGetJourneyAnswers(mtditid, taxYear, "close-company-loans", None)
        mockGetDividendsIncomeDataSuccess(nino, taxYear, emptyStockDividendsResult)

        val tasks = Seq(
          TaskListSectionItem(CashDividends, NotStarted, Some(ukDividendsUrl)),
          TaskListSectionItem(DividendsFromUnitTrusts, Completed, Some(otherUkDividendsUrl))
        )

        def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
        result mustBe TaskListSection(SectionTitle.DividendsTitle, Some(tasks))
      }

      "return the appropriate status when the status field can be parsed" in {
        val journeyAnswers = JourneyAnswers(mtditid, taxYear, "cash-dividends", Json.obj("status" -> JsString("completed")), Instant.now())
        mockGetJourneyAnswers(mtditid, taxYear, "cash-dividends", Some(journeyAnswers))
        mockGetJourneyAnswers(mtditid, taxYear, "dividends-from-unit-trusts", None)
        mockGetSubmittedDividendsSuccess(nino, taxYear, fullDividendsResult)

        mockGetJourneyAnswers(mtditid, taxYear, "stock-dividends", None)
        mockGetJourneyAnswers(mtditid, taxYear, "free-redeemable-shares", None)
        mockGetJourneyAnswers(mtditid, taxYear, "close-company-loans", None)
        mockGetDividendsIncomeDataSuccess(nino, taxYear, emptyStockDividendsResult)

        val tasks = Seq(
          TaskListSectionItem(CashDividends, Completed, Some(ukDividendsUrl)),
          TaskListSectionItem(DividendsFromUnitTrusts, Completed, Some(otherUkDividendsUrl))
        )

        def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
        result mustBe TaskListSection(SectionTitle.DividendsTitle, Some(tasks))
      }

      "when journey answers are not defined, and data exists for dividends in DES" should {
        "return 'Completed' status" in {
          mockGetJourneyAnswers(mtditid, taxYear, "cash-dividends", None)
          mockGetJourneyAnswers(mtditid, taxYear, "dividends-from-unit-trusts", None)
          mockGetSubmittedDividendsSuccess(nino, taxYear, fullDividendsResult)

          mockGetJourneyAnswers(mtditid, taxYear, "stock-dividends", None)
          mockGetJourneyAnswers(mtditid, taxYear, "free-redeemable-shares", None)
          mockGetJourneyAnswers(mtditid, taxYear, "close-company-loans", None)
          mockGetDividendsIncomeDataSuccess(nino, taxYear, fullStockDividendsResult)

          val tasks = Seq(
            TaskListSectionItem(CashDividends, Completed, Some(ukDividendsUrl)),
            TaskListSectionItem(DividendsFromUnitTrusts, Completed, Some(otherUkDividendsUrl)),
            TaskListSectionItem(StockDividends, Completed, Some(stockDividendsUrl)),
            TaskListSectionItem(FreeRedeemableShares, Completed, Some(redeemableSharesUrl)),
            TaskListSectionItem(CloseCompanyLoans, Completed, Some(closeCompanyUrl))
          )

          def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
          result mustBe TaskListSection(SectionTitle.DividendsTitle, Some(tasks))
        }
      }

      "when journey answers are not defined, and data does not exist for dividends in DES" should {
        "return 'Completed' status" in {
          val emptyDividendsResult: SubmittedDividendsResponse = Right(SubmittedDividendsModel(None, None, None))

          val emptyStockDividendsResult: GetDividendsIncomeDataResponse = Right(DividendsIncomeDataModel(
            None, None, None, None, None, None, None
          ))

          mockGetJourneyAnswers(mtditid, taxYear, "cash-dividends", None)
          mockGetJourneyAnswers(mtditid, taxYear, "dividends-from-unit-trusts", None)
          mockGetSubmittedDividendsSuccess(nino, taxYear, emptyDividendsResult)

          mockGetJourneyAnswers(mtditid, taxYear, "stock-dividends", None)
          mockGetJourneyAnswers(mtditid, taxYear, "free-redeemable-shares", None)
          mockGetJourneyAnswers(mtditid, taxYear, "close-company-loans", None)
          mockGetDividendsIncomeDataSuccess(nino, taxYear, emptyStockDividendsResult)

          def result: TaskListSection = await(service.get(taxYear, nino, mtditid))
          result mustBe TaskListSection(SectionTitle.DividendsTitle, None)
        }
      }
    }
  }
}
