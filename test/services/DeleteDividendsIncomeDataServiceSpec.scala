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

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.when

import connectors.{DeleteDividendsIncomeDataConnector, DeleteDividendsIncomeDataTYSConnector}
import connectors.httpParsers.DeleteDividendsIncomeParser.DeleteDividendsIncomeResponse
import connectors.httpParsers.DeleteDividendsIncomeTYSParser.DeleteDividendsIncomeTYSResponse
import utils.{TaxYearUtils, TestUtils}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class DeleteDividendsIncomeDataServiceSpec extends TestUtils {
  val connector: DeleteDividendsIncomeDataConnector = mock[DeleteDividendsIncomeDataConnector]
  val connectorTYS: DeleteDividendsIncomeDataTYSConnector = mock[DeleteDividendsIncomeDataTYSConnector]
  val service: DeleteDividendsIncomeDataService = new DeleteDividendsIncomeDataService(connector, connectorTYS)

  private val specificTaxYear: Int = TaxYearUtils.specificTaxYear
  private val specificTaxYearPlusOne: Int = specificTaxYear + 1

  "DeleteDividendsIncomeDataServiceSpec" should {

    "return the connector response" in {

      val expectedResult: DeleteDividendsIncomeResponse = Right(true)

      when(connector.deleteDividendsIncomeData(eqTo("12345678"), eqTo(taxYear))(any[HeaderCarrier]())).thenReturn(Future.successful(expectedResult))

      val result = await(service.deleteDividendsIncomeData("12345678", taxYear))

      result mustBe expectedResult
    }
  }

  "DeleteDividendsIncomeData for specific tax year" should {

    "return the connector response" in {

      val expectedResult: DeleteDividendsIncomeTYSResponse = Right(true)

      when(connectorTYS.deleteDividendsIncomeData(eqTo("12345678"), eqTo(specificTaxYear))(any[HeaderCarrier]())).thenReturn(Future.successful(expectedResult))

      val result = await(service.deleteDividendsIncomeData("12345678", specificTaxYear))

      result mustBe expectedResult
    }
  }

  "DeleteDividendsIncomeData for specific tax year plus one" should {

    "return the connector response" in {

      val expectedResult: DeleteDividendsIncomeTYSResponse = Right(true)

      when(connectorTYS.deleteDividendsIncomeData(eqTo("12345678"), eqTo(specificTaxYearPlusOne))(any[HeaderCarrier]())).thenReturn(Future.successful(expectedResult))

      val result = await(service.deleteDividendsIncomeData("12345678", specificTaxYearPlusOne))

      result mustBe expectedResult
    }
  }
}
