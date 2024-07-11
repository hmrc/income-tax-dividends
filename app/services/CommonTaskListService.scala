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

import config.AppConfig
import models.taskList._
import models.{AllDividends, DividendsIncomeDataModel, SubmittedDividendsModel}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CommonTaskListService @Inject()(appConfig: AppConfig,
                                      dividendsService: SubmittedDividendsService,
                                      stockDividendsService: GetDividendsIncomeService) {

  def get(taxYear: Int, nino: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[TaskListSection] = {

    val dividends: Future[SubmittedDividendsModel] = dividendsService.getSubmittedDividends(nino, taxYear).map {
      case Left(_) => SubmittedDividendsModel(None, None)
      case Right(value) => value
    }

    val stockDividends: Future[DividendsIncomeDataModel] = stockDividendsService.getDividendsIncomeData(nino, taxYear).map {
      case Left(_) => DividendsIncomeDataModel(None, None, None, None, None, None, None)
      case Right(value) => value
    }

    val allDividends: Future[AllDividends] = for (
      ukDividends <- dividends.map(_.ukDividends);
      otherUkDividends <- dividends.map(_.otherUkDividends);
      stockDividend <- stockDividends.map(_.stockDividend);
      redeemable <- stockDividends.map(_.redeemableShares);
      closeCompany <- stockDividends.map(_.closeCompanyLoansWrittenOff)
    ) yield {
      AllDividends(
        SubmittedDividendsModel(ukDividends, otherUkDividends, deletedPeriod = None),
        DividendsIncomeDataModel(None, None, None, stockDividend, redeemable, None, closeCompany)
      )
    }

    allDividends.map { d =>

      val tasks: Option[Seq[TaskListSectionItem]] = {

        val optionalTasks: Seq[TaskListSectionItem] = getTasks(d.dividends, d.stockDividends, taxYear)

        if (optionalTasks.nonEmpty) {
          Some(optionalTasks)
        } else {
          None
        }
      }

      TaskListSection(SectionTitle.DividendsTitle, tasks)
    }
  }

  private def getTasks(d: SubmittedDividendsModel, sd: DividendsIncomeDataModel, taxYear: Int): Seq[TaskListSectionItem] = {

    // TODO: these will be links to the new CYA pages when they are made
    val ukDividendsUrl: String = s"${appConfig.personalFrontendBaseUrl}/$taxYear/dividends/how-much-dividends-from-uk-companies"
    val otherUkDividendsUrl: String =
      s"${appConfig.personalFrontendBaseUrl}/$taxYear/dividends/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"
    val stockDividendsUrl: String = s"${appConfig.personalFrontendBaseUrl}/$taxYear/dividends/stock-dividend-amount"
    val redeemableUrl: String = s"${appConfig.personalFrontendBaseUrl}/$taxYear/dividends/redeemable-shares-amount"
    val closeCompanyUrl: String = s"${appConfig.personalFrontendBaseUrl}/$taxYear/dividends/close-company-loan-amount"

    val ukDividends: Option[TaskListSectionItem] = if (d.ukDividends.isDefined) {
      Some(TaskListSectionItem(TaskTitle.CashDividends, TaskStatus.Completed, Some(ukDividendsUrl)))
    } else {
      None
    }

    val otherUkDividends: Option[TaskListSectionItem] = if (d.otherUkDividends.isDefined) {
      Some(TaskListSectionItem(TaskTitle.DividendsFromUnitTrusts, TaskStatus.Completed, Some(otherUkDividendsUrl)))
    } else {
      None
    }

    val stockDividends: Option[TaskListSectionItem] = if (sd.stockDividend.isDefined) {
      Some(TaskListSectionItem(TaskTitle.StockDividends, TaskStatus.Completed, Some(stockDividendsUrl)))
    } else {
      None
    }

    val redeemable: Option[TaskListSectionItem] = if (sd.redeemableShares.isDefined) {
      Some(TaskListSectionItem(TaskTitle.FreeRedeemableShares, TaskStatus.Completed, Some(redeemableUrl)))
    } else {
      None
    }

    val closeCompany: Option[TaskListSectionItem] = if (sd.closeCompanyLoansWrittenOff.isDefined) {
      Some(TaskListSectionItem(TaskTitle.CloseCompanyLoans, TaskStatus.Completed, Some(closeCompanyUrl)))
    } else {
      None
    }

    Seq[Option[TaskListSectionItem]](ukDividends, otherUkDividends, stockDividends, redeemable, closeCompany).flatten
  }
}