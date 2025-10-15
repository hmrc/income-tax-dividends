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
import connectors.httpParsers.CreateUpdateAnnualIncomeSourceHttpParser.{CreateUpdateAnnualIncomeSourceHttpReads, CreateUpdateAnnualIncomeSourceResponse}
import models.CreateOrAmendDividendsModel
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.TaxYearUtils.convertSpecificTaxYear

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateUpdateAnnualIncomeSourceConnector @Inject()(http: HttpClientV2, val appConfig: AppConfig)(implicit ec: ExecutionContext) extends IFConnector{
  def createUpdateAnnualIncomeSource(
                                      nino: String, taxYear: Int, dividendsModel: CreateOrAmendDividendsModel
                            )(implicit hc: HeaderCarrier): Future[CreateUpdateAnnualIncomeSourceResponse] = {
    val formattedTaxYear: String = convertSpecificTaxYear(taxYear)
    val createUpdateAnnualIncomeSourceUrl: String = appConfig.ifBaseUrl + s"/income-tax/$formattedTaxYear/$nino/income-source/dividends/annual"

    def ifCall(implicit hc: HeaderCarrier): Future[CreateUpdateAnnualIncomeSourceResponse] = {
      http.post(url"$createUpdateAnnualIncomeSourceUrl").
        withBody(Json.toJson(dividendsModel)).
        execute[CreateUpdateAnnualIncomeSourceResponse]
    }

    ifCall(ifHeaderCarrier(createUpdateAnnualIncomeSourceUrl, "1784"))
  }
}
