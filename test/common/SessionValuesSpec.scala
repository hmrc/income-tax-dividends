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

package common

import utils.TestUtils

class SessionValuesSpec extends TestUtils {

  "SessionValues" should {

    "return the correct string for mtditid" in {
      SessionValues.CLIENT_MTDITID mustBe "ClientMTDID"
    }

    "return the correct string for nino" in {
      SessionValues.CLIENT_NINO mustBe "ClientNino"
    }

    "return the correct string for tax year" in {
      SessionValues.TAX_YEAR mustBe "TAX_YEAR"
    }

    "return the correct string for valid tax years" in {
      SessionValues.VALID_TAX_YEARS mustBe "validTaxYears"
    }
  }
}
