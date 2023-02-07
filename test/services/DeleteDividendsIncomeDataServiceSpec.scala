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

import connectors.DeleteDividendsIncomeDataConnector
import connectors.httpParsers.DeleteDividendsIncomeParser.DeleteDividendsIncomeResponse
import utils.TestUtils
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class DeleteDividendsIncomeDataServiceSpec extends TestUtils {
  val connector: DeleteDividendsIncomeDataConnector = mock[DeleteDividendsIncomeDataConnector]
  val service: DeleteDividendsIncomeDataService = new DeleteDividendsIncomeDataService(connector)

  "DeleteDividendsIncomeData" should {

    "return the connector response" in {

      val expectedResult: DeleteDividendsIncomeResponse = Right(true)

      (connector.deleteDividendsIncomeData(_: String, _: Int)(_: HeaderCarrier))
        .expects("12345678", 1234, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.deleteDividendsIncomeData("12345678", 1234))

      result mustBe expectedResult

    }
  }
}
