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

package services

import connectors.{CreateUpdateStockDividendsIncomeConnector, CreateUpdateStockDividendsIncomeTYSConnector}
import connectors.httpParsers.CreateUpdateStockDividendsIncomeHttpParser.CreateUpdateStockDividendsIncomeResponse
import models.StockDividendsSubmissionModel
import uk.gov.hmrc.http.HeaderCarrier
import utils.TaxYearUtils.specificTaxYear

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class CreateUpdateDividendsIncomeService @Inject()(createUpdateDividendsIncomeConnector: CreateUpdateStockDividendsIncomeConnector,
                                                   createUpdateDividendsIncomeTYSConnector: CreateUpdateStockDividendsIncomeTYSConnector) {

  def createUpdateDividends(nino: String, taxYear: Int, createUpdateDividendsModel: StockDividendsSubmissionModel)
                           (implicit hc: HeaderCarrier): Future[CreateUpdateStockDividendsIncomeResponse] = {
    if (taxYear >= specificTaxYear) {
      createUpdateDividendsIncomeTYSConnector.createUpdateDividends(nino, taxYear, createUpdateDividendsModel)
    } else {
      createUpdateDividendsIncomeConnector.createUpdateDividends(nino, taxYear, createUpdateDividendsModel)
    }
  }

}
