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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import services.SubmittedDividendsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockSubmittedDividendsService extends MockitoSugar {
  val mockSubmittedDividendsService: SubmittedDividendsService = mock[SubmittedDividendsService]

  def mockGetSubmittedDividendsSuccess(nino: String, taxYear: Int, result: SubmittedDividendsResponse): Unit =
    when(mockSubmittedDividendsService.getSubmittedDividends(eqTo(nino), eqTo(taxYear))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockGetSubmittedDividendsFailure(nino: String, taxYear: Int, result: Throwable): Unit =
    when(mockSubmittedDividendsService.getSubmittedDividends(eqTo(nino), eqTo(taxYear))(any[HeaderCarrier]()))
      .thenReturn(Future.failed(result))
}
