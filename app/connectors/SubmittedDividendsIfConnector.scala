/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.httpParsers.SubmittedDividendsIfHttpParser._
import connectors.httpParsers.SubmittedDividendsIfHttpParser.SubmittedDividendsIfResponse
import play.api.routing.sird.?

import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class SubmittedDividendsIfConnector @Inject() (val http: HttpClient,
                                             val appConfig: AppConfig)(implicit ec:ExecutionContext) extends IFConnector {

  def getSubmittedDividends(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[SubmittedDividendsIfResponse] = {
    val taxYearParameter = s"${taxYear - 1}-${taxYear.toString takeRight 2}"
    val incomeSourcesUri: String = appConfig.ifBaseUrl + s"/income-tax/$taxYearParameter/$nino/income-source/dividends/annual?deleteReturnPeriod=false"
    http.GET[SubmittedDividendsIfResponse](incomeSourcesUri)
  }
}