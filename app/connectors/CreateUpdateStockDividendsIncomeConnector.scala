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

package connectors

import com.typesafe.config.ConfigFactory
import config.AppConfig
import connectors.httpParsers.CreateUpdateStockDividendsIncomeHttpParser.{CreateUpdateDividendsIncomeHttpReads, CreateUpdateStockDividendsIncomeResponse}
import models.StockDividendsSubmissionModel
import uk.gov.hmrc.http.HeaderCarrier.Config
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.TaxYearUtils.convertStringTaxYear

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateUpdateStockDividendsIncomeConnector @Inject()(http: HttpClient, val appConfig: AppConfig)(implicit ec: ExecutionContext) extends IFConnector {

  override val headerCarrierConfig: Config = HeaderCarrier.Config.fromConfig(ConfigFactory.load())

  def createUpdateDividends(nino: String, taxYear: Int, createUpdatedDividendsIncomeModel: StockDividendsSubmissionModel)
                           (implicit hc: HeaderCarrier): Future[CreateUpdateStockDividendsIncomeResponse] = {
    val putDividendsIncome = "1906"
    val taxYearParameter = convertStringTaxYear(taxYear)
    val url = appConfig.ifBaseUrl + s"/income-tax/income/dividends/$nino/$taxYearParameter"

    http.PUT[StockDividendsSubmissionModel, CreateUpdateStockDividendsIncomeResponse](url, createUpdatedDividendsIncomeModel)(
      StockDividendsSubmissionModel.formats.writes, CreateUpdateDividendsIncomeHttpReads, ifHeaderCarrier(url, putDividendsIncome), ec)
  }
}
