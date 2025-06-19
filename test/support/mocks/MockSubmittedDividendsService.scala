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

package support.mocks

import connectors.httpParsers.SubmittedDividendsHttpParser.SubmittedDividendsResponse
import org.scalamock.handlers.CallHandler3
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import services.SubmittedDividendsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockSubmittedDividendsService extends MockFactory { _: TestSuite =>
  private type GetSubmittedDividendsMockResult = CallHandler3[String, Int, HeaderCarrier, Future[SubmittedDividendsResponse]]
  val mockSubmittedDividendsService: SubmittedDividendsService = mock[SubmittedDividendsService]

  def mockGetSubmittedDividendsSuccess(nino: String,
                                        taxYear: Int,
                                        result: SubmittedDividendsResponse): GetSubmittedDividendsMockResult =
    (mockSubmittedDividendsService.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
      .expects(nino, taxYear, *)
      .returning(Future.successful(result))

  def mockGetSubmittedDividendsFailure(nino: String,
                                        taxYear: Int,
                                        result: Throwable): GetSubmittedDividendsMockResult =
    (mockSubmittedDividendsService.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
      .expects(nino, taxYear, *)
      .returning(Future.failed(result))
}
