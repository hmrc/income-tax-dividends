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

package models.priorDataModels

import models.{DividendsIncomeDataModel, StockDividendModel, SubmittedDividendsModel}
import utils.TestUtils

class StockDividendsPriorDataModelSpec extends TestUtils {
  private val monetaryValue: BigDecimal = 123.45
  private val stockDividend: StockDividendModel = StockDividendModel(None, monetaryValue)

 ".getFromPrior" should {

   "populate the model correctly with all values" in {

     val result = StockDividendsPriorDataModel.getFromPrior(
       SubmittedDividendsModel(completeDividendsCYAModel.ukDividendsAmount, completeDividendsCYAModel.otherUkDividendsAmount),
       Some(DividendsIncomeDataModel(
         None,
         None,
         None,
         stockDividend = Some(stockDividend),
         redeemableShares = Some(stockDividend),
         None,
         closeCompanyLoansWrittenOff = Some(stockDividend))
       )
     )

     result mustBe StockDividendsPriorDataModel(Some(50.0), Some(50.0), Some(123.45), Some(123.45), Some(123.45))
   }

   "populate the model correctly with no values" in {

     val result = StockDividendsPriorDataModel.getFromPrior(
       SubmittedDividendsModel(None, None),
       None
     )

     result mustBe StockDividendsPriorDataModel(None, None, None, None, None)
   }
 }
}
