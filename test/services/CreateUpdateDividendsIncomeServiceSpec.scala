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

import com.codahale.metrics.SharedMetricRegistries
import connectors.httpParsers.CreateUpdateDividendsIncomeHttpParser.CreateUpdateDividendsIncomeResponse
import models.{DividendsIncomeDataModel, ForeignDividendModel, StockDividendModel}
import connectors.CreateUpdateDividendsIncomeConnector
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class CreateUpdateDividendsIncomeServiceSpec extends TestUtils {
  SharedMetricRegistries.clear()

  val connector: CreateUpdateDividendsIncomeConnector = mock[CreateUpdateDividendsIncomeConnector]
  val service: CreateUpdateDividendsIncomeService = new CreateUpdateDividendsIncomeService(connector)

  val nino: String = "123456789"
  val taxYear: Int = 1999
  val reference: String = "RefNo13254687"
  val countryCode: String = "GBR"
  val decimalValue: BigDecimal = 123.45
  val model: DividendsIncomeDataModel = DividendsIncomeDataModel(
    submittedOn = None,
    foreignDividend =
      Some(Seq(
        ForeignDividendModel(countryCode, Some(decimalValue), Some(decimalValue), Some(decimalValue), Some(true), decimalValue)
      ))
    ,
    dividendIncomeReceivedWhilstAbroad = Some(
      Seq(
        ForeignDividendModel(countryCode, Some(decimalValue), Some(decimalValue), Some(decimalValue), Some(true), decimalValue)
      )
    ),
    stockDividend = Some(StockDividendModel(Some(reference), decimalValue)),
    redeemableShares = Some(StockDividendModel(Some(reference), decimalValue)),
    bonusIssuesOfSecurities = Some(StockDividendModel(Some(reference), decimalValue)),
    closeCompanyLoansWrittenOff = Some(StockDividendModel(Some(reference), decimalValue))
  )


  ".createUpdateDividends" should {

    "return the IF connector response" in {

      val expectedResult: CreateUpdateDividendsIncomeResponse = Right(model)

      (connector.createUpdateDividends(_: String, _: Int, _: DividendsIncomeDataModel)(_: HeaderCarrier))
        .expects(nino, taxYear, *, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.createUpdateDividends(nino, taxYear, model))

      result mustBe expectedResult

    }
  }
}
