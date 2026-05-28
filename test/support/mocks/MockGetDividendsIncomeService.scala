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

import connectors.httpParsers.GetDividendsIncomeParser.GetDividendsIncomeDataResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import services.GetDividendsIncomeService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockGetDividendsIncomeService extends MockitoSugar {
  val mockGetDividendsIncomeService: GetDividendsIncomeService = mock[GetDividendsIncomeService]

  def mockGetDividendsIncomeDataSuccess(nino: String, taxYear: Int, result: GetDividendsIncomeDataResponse): Unit =
    when(mockGetDividendsIncomeService.getDividendsIncomeData(eqTo(nino), eqTo(taxYear))(any[HeaderCarrier]()))
      .thenReturn(Future.successful(result))

  def mockGetDividendsIncomeDataFailure(nino: String, taxYear: Int, result: Throwable): Unit =
    when(mockGetDividendsIncomeService.getDividendsIncomeData(eqTo(nino), eqTo(taxYear))(any[HeaderCarrier]()))
      .thenReturn(Future.failed(result))
}
