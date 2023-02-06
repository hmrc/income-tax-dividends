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

import connectors.GetDividendsIncomeConnector
import connectors.httpParsers.GetDividendsIncomeParser.GetDividendsIncomeDataResponse
import models._
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class GetDividendsIncomeServiceSpec extends TestUtils {

  val connector: GetDividendsIncomeConnector = mock[GetDividendsIncomeConnector]
  val service: GetDividendsIncomeService = new GetDividendsIncomeService(connector)
  val nino: String = "12345678"
  val taxYear: Int = 1234

  ".getDividendsIncomeData" should {

    "return the connector response" in {

      val expectedResult: GetDividendsIncomeDataResponse = Right(DividendsIncomeDataModel(
        submittedOn = Some("2020-06-17T10:53:38Z"),
        foreignDividend = Some(Seq(ForeignDividendModel("BES", Some(2323.56), Some(5435.56), Some(4564.67), Some(true), 4564.67))),
        dividendIncomeReceivedWhilstAbroad = Some(Seq(ForeignDividendModel("CHN", Some(5664.67), Some(5657.56),
          Some(4644.56), Some(true), 4654.56))),
        stockDividend = Some(StockDividendModel(Some("Stock Dividend Customer Reference"), 2525.89)),
        redeemableShares = Some(StockDividendModel(Some("Redeemable Shares Customer Reference"), 3535.56)),
        bonusIssuesOfSecurities = Some(StockDividendModel(Some("Bonus Issues Of Securities Customer Reference"), 5633.67)),
        closeCompanyLoansWrittenOff = Some(StockDividendModel(Some("Close Company Loans WrittenOff Customer Reference"), 6743.23))
      ))

      (connector.getDividendsIncomeData(_: String, _: Int)(_: HeaderCarrier))
        .expects(nino, taxYear, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.getDividendsIncomeData(nino, taxYear))

      result mustBe expectedResult

    }
  }
}
