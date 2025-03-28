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

package connectors

import config.AppConfig
import connectors.httpParsers.DeleteDividendsIncomeTYSParser.{DeleteDividendsIncomeTYSHttpReads, DeleteDividendsIncomeTYSResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.TaxYearUtils.convertSpecificTaxYear

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteDividendsIncomeDataTYSConnector @Inject()(http: HttpClient, val appConfig: AppConfig)(implicit ec: ExecutionContext) extends IFConnector {

  val deleteDividendsIncomeTYS = "1908"

  def deleteDividendsIncomeData(nino: String,
                                       taxYear: Int)(implicit hc: HeaderCarrier): Future[DeleteDividendsIncomeTYSResponse] = {

    val taxYearParameter = convertSpecificTaxYear(taxYear)
    val deleteDividendsIncomeTYSUrl: String = appConfig.ifBaseUrl + s"/income-tax/income/dividends/$taxYearParameter/$nino"

    http.DELETE[DeleteDividendsIncomeTYSResponse](deleteDividendsIncomeTYSUrl)(DeleteDividendsIncomeTYSHttpReads,
      ifHeaderCarrier(deleteDividendsIncomeTYSUrl, deleteDividendsIncomeTYS), ec)
  }
}