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
import org.scalamock.handlers.CallHandler3
import org.scalamock.scalatest.MockFactory
import services.GetDividendsIncomeService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockGetDividendsIncomeService extends MockFactory {
  private type GetDividendsIncomeMockResult = CallHandler3[String, Int, HeaderCarrier, Future[GetDividendsIncomeDataResponse]]
  val mockGetDividendsIncomeService: GetDividendsIncomeService = mock[GetDividendsIncomeService]

  def mockGetDividendsIncomeDataSuccess(nino: String,
                                        taxYear: Int,
                                        result: GetDividendsIncomeDataResponse): GetDividendsIncomeMockResult =
    (mockGetDividendsIncomeService.getDividendsIncomeData(_: String, _: Int)(_: HeaderCarrier))
      .expects(nino, taxYear, *)
      .returning(Future.successful(result))

  def mockGetDividendsIncomeDataFailure(nino: String,
                                        taxYear: Int,
                                        result: Throwable): GetDividendsIncomeMockResult =
    (mockGetDividendsIncomeService.getDividendsIncomeData(_: String, _: Int)(_: HeaderCarrier))
      .expects(nino, taxYear, *)
      .returning(Future.failed(result))

}
