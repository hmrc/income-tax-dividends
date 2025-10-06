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
import connectors.httpParsers.DeleteDividendsIncomeParser.{DeleteDividendsIncomeHttpReads, DeleteDividendsIncomeResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.TaxYearUtils.convertStringTaxYear

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteDividendsIncomeDataConnector @Inject()(http: HttpClientV2, val appConfig: AppConfig)(implicit ec: ExecutionContext) extends IFConnector {

  val deleteDividendsIncome = "1610"

  def deleteDividendsIncomeData(nino: String,
                                       taxYear: Int)(implicit hc: HeaderCarrier): Future[DeleteDividendsIncomeResponse] = {

    val taxYearParameter = convertStringTaxYear(taxYear)
    val deleteDividendsIncomeUrl: String = appConfig.ifBaseUrl + s"/income-tax/income/dividends/$nino/$taxYearParameter"

    http.delete(url"$deleteDividendsIncomeUrl")(ifHeaderCarrier(deleteDividendsIncomeUrl, deleteDividendsIncome))
      .execute[DeleteDividendsIncomeResponse]
  }
}