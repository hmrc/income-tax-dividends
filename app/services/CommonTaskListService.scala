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

import cats.data.EitherT
import config.AppConfig
import models.ErrorModel
import models.mongo.JourneyAnswers
import models.taskList.TaskStatus.{Completed, InProgress, NotStarted}
import models.taskList.{SectionTitle, TaskListSection, TaskListSectionItem, TaskStatus, TaskTitle}
import play.api.Logging
import repositories.JourneyAnswersRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommonTaskListService @Inject()(
                                       appConfig: AppConfig,
                                       dividendsService: SubmittedDividendsService,
                                       stockDividendsService: GetDividendsIncomeService,
                                       journeyAnswersRepository: JourneyAnswersRepository
                                     ) extends Logging {

  private lazy val baseUrl = s"${appConfig.personalFrontendBaseUrl}"

  private def getTaskForItem(
                              taskTitle: TaskTitle,
                              taskUrl: String,
                              journeyAnswers: Option[JourneyAnswers],
                              isDataDefined: Boolean
                            ): Option[TaskListSectionItem] =
    (journeyAnswers, isDataDefined) match {
      case (Some(ja), _) =>
        val status: TaskStatus = ja.data.value("status").validate[TaskStatus].asOpt match {
          case Some(TaskStatus.Completed) => Completed
          case Some(TaskStatus.InProgress) => InProgress
          case _ =>
            logger.info("[CommonTaskListService][getStatus] status stored in an invalid format, setting as 'Not yet started'.")
            NotStarted
        }
        Some(TaskListSectionItem(taskTitle, status, Some(taskUrl)))
      case (_, true) => Some(TaskListSectionItem(taskTitle, if(appConfig.sectionCompletedQuestionEnabled) InProgress else Completed, Some(taskUrl)))
      case _ => None
    }

  private def getDividendTasks(taxYear: Int, nino: String, mtdItId: String)
                              (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Seq[TaskListSectionItem]] = {
    val ukDividendsUrl = s"$baseUrl/$taxYear/dividends/check-how-much-dividends-from-uk-companies"
    val otherUkDividendsUrl = s"$baseUrl/$taxYear/dividends/check-how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"
    val ukDividendsJourneyName = "cash-dividends"
    val otherUkDividendsJourneyName = "dividends-from-unit-trusts"

    val result: EitherT[Future, ErrorModel, Seq[TaskListSectionItem]] = {
      for {
        ukJourneyAnswers <- EitherT.right(journeyAnswersRepository.get(mtdItId, taxYear, ukDividendsJourneyName))
        otherJourneyAnswers <- EitherT.right(journeyAnswersRepository.get(mtdItId, taxYear, otherUkDividendsJourneyName))
        dividends <- EitherT(dividendsService.getSubmittedDividends(nino, taxYear))
      } yield Seq(
        getTaskForItem(TaskTitle.CashDividends, ukDividendsUrl, ukJourneyAnswers, dividends.ukDividends.isDefined),
        getTaskForItem(TaskTitle.DividendsFromUnitTrusts, otherUkDividendsUrl, otherJourneyAnswers, dividends.otherUkDividends.isDefined)
      ).flatten
    }

    result.leftMap(_ => Nil).merge
  }

  private def getStockDividendTasks(taxYear: Int, nino: String, mtdItId: String)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Seq[TaskListSectionItem]] = {
    val stockDividendsUrl = s"$baseUrl/$taxYear/dividends/check-stock-dividend-amount"
    val redeemableSharesUrl = s"$baseUrl/$taxYear/dividends/check-redeemable-shares-amount"
    val closeCompanyUrl = s"$baseUrl/$taxYear/dividends/check-close-company-loan-amount"
    val stockJourneyName = "stock-dividends"
    val redeemableJourneyName = "free-redeemable-shares"
    val closeCompanyJourneyName = "close-company-loans"

    val result: EitherT[Future, ErrorModel, Seq[TaskListSectionItem]] = {
      for {
        stockJourneyAnswers <- EitherT.right(journeyAnswersRepository.get(mtdItId, taxYear, stockJourneyName))
        redeemableJourneyAnswers <- EitherT.right(journeyAnswersRepository.get(mtdItId, taxYear, redeemableJourneyName))
        closeCompanyJourneyAnswers <- EitherT.right(journeyAnswersRepository.get(mtdItId, taxYear, closeCompanyJourneyName))
        stockDividends <- EitherT(stockDividendsService.getDividendsIncomeData(nino, taxYear))
      } yield Seq(
        getTaskForItem(TaskTitle.StockDividends, stockDividendsUrl, stockJourneyAnswers, stockDividends.stockDividend.isDefined),
        getTaskForItem(TaskTitle.FreeRedeemableShares, redeemableSharesUrl, redeemableJourneyAnswers, stockDividends.redeemableShares.isDefined),
        getTaskForItem(TaskTitle.CloseCompanyLoans, closeCompanyUrl, closeCompanyJourneyAnswers, stockDividends.closeCompanyLoansWrittenOff.isDefined)
      ).flatten
    }

    result.leftMap(_ => Nil).merge
  }

  def get(taxYear: Int, nino: String, mtdItId: String)(
    implicit ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[TaskListSection] = {
    val result = for {
      dividendTasks <- getDividendTasks(taxYear, nino, mtdItId)
      stockDividendTasks <- getStockDividendTasks(taxYear, nino, mtdItId)
      allTasks = dividendTasks ++ stockDividendTasks
    } yield {
      val tasks = if (allTasks.nonEmpty) Some(allTasks) else None
      TaskListSection(SectionTitle.DividendsTitle, tasks)
    }

    result
  }
}
