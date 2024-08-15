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

package models

import utils.TestUtils

class IncomeSourcesSpec extends TestUtils {

 "IncomeSources" should {

   "return the string value of stock dividends" in {

     val result = IncomeSources.STOCK_DIVIDENDS

     result mustBe "stock-dividends"
   }

   "return the string value of dividends" in {

     val result = IncomeSources.DIVIDENDS

     result mustBe "dividends"
   }
 }
}
