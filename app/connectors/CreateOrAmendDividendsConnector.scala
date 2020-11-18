/*
 * Copyright 2020 HM Revenue & Customs
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
import connectors.httpParsers.CreateOrAmendDividendsHttpParser.CreateOrAmendDividendsResponse
import javax.inject.Inject
import models.CreateOrAmendDividendsModel
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class CreateOrAmendDividendsConnector @Inject()(http: HttpClient, config: AppConfig)(implicit ec: ExecutionContext) {

  def createOrAmendDividends(
                             nino: String, taxYear: Int, dividendsModel: CreateOrAmendDividendsModel
                           )(implicit hc: HeaderCarrier): Future[CreateOrAmendDividendsResponse] = {
    val createOrAmendDividendsUrl: String = config.desBaseUrl + s"/income-tax/nino/$nino/income-source/dividends/" +
      s"annual/$taxYear"
    http.POST[CreateOrAmendDividendsModel, CreateOrAmendDividendsResponse](createOrAmendDividendsUrl, dividendsModel)
  }

}
