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

package models

import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import utils.TestUtils

class UserSpec extends TestUtils {

  ".isAgent" should {

    "return true" when {

      "user has an arn" in {
        User[AnyContent]("23456789", Some("123456789"), "AA123456A", "sessionId-eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")(FakeRequest()).isAgent mustBe true
      }

    }

    "return false" when {

      "user does not have an arn" in {
        User[AnyContent]("23456789", None, "AA123456A", "sessionId-eb3158c2-0aff-4ce8-8d1b-f2208ace52fe")(FakeRequest()).isAgent mustBe false
      }

    }

  }

}
