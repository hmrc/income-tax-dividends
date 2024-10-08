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

import connectors.{GetAnnualIncomeSourcePeriodConnector, SubmittedDividendsConnector}

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import connectors.httpParsers.SubmittedDividendsHttpParser.SubmittedDividendsResponse
import utils.TaxYearUtils.specificTaxYear

import scala.concurrent.Future

@Singleton
class SubmittedDividendsService @Inject()(submittedDividendsConnector: SubmittedDividendsConnector,
                                          submittedDividendsIfConnector: GetAnnualIncomeSourcePeriodConnector) {
  def getSubmittedDividends(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[SubmittedDividendsResponse] = {
    if (taxYear >= specificTaxYear) {
      submittedDividendsIfConnector.getAnnualIncomeSourcePeriod(nino, taxYear, Some(false))
    }
    else {
      submittedDividendsConnector.getSubmittedDividends(nino, taxYear)
    }
  }
}
