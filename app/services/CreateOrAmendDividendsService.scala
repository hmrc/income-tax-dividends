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

import connectors.{CreateOrAmendDividendsConnector, CreateUpdateAnnualIncomeSourceConnector}
import connectors.httpParsers.CreateOrAmendDividendsHttpParser.CreateOrAmendDividendsResponse

import javax.inject.{Inject, Singleton}
import models.CreateOrAmendDividendsModel
import uk.gov.hmrc.http.HeaderCarrier
import utils.TaxYearUtils.specificTaxYear

import scala.concurrent.Future

@Singleton
class CreateOrAmendDividendsService @Inject()(createOrAmendDividendsConnector: CreateOrAmendDividendsConnector,
                                              createUpdateAnnualIncomeSourceConnector: CreateUpdateAnnualIncomeSourceConnector) {
  def createOrAmendDividends(
                             nino: String, taxYear: Int, dividendsModel: CreateOrAmendDividendsModel
                           )(implicit hc: HeaderCarrier): Future[CreateOrAmendDividendsResponse] = {

    if (taxYear >= specificTaxYear) {
      createUpdateAnnualIncomeSourceConnector.createUpdateAnnualIncomeSource(nino, taxYear, dividendsModel)
    } else {
      createOrAmendDividendsConnector.createOrAmendDividends(nino, taxYear, dividendsModel)
    }
  }

}
