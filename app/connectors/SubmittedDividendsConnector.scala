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

import config.AppConfig
import connectors.httpParsers.SubmittedDividendsHttpParser._
import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class SubmittedDividendsConnector @Inject() (val http: HttpClient,
                                             val appConfig: AppConfig)(implicit ec:ExecutionContext) extends DesConnector {

  def getSubmittedDividends(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[SubmittedDividendsResponse] = {
    val incomeSourcesUri: String = appConfig.desBaseUrl + s"/income-tax/nino/$nino/income-source/dividends/" +
      s"annual/$taxYear"

    def desCall(implicit hc: HeaderCarrier): Future[SubmittedDividendsResponse] = {
      http.GET[SubmittedDividendsResponse](incomeSourcesUri)
    }

    desCall(desHeaderCarrier(incomeSourcesUri))
  }
}
