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

package utils

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.crypto.EncryptedValue
import utils.Cypher.{bigDecimalCypher, booleanCypher, monthCypher, stringCypher}

import java.time.Month

class CypherSpec extends AnyWordSpec with MockitoSugar with Matchers {

  private val encryptedBoolean = EncryptedValue("encrypted-boolean", "boolean-nonce")
  private val encryptedString = EncryptedValue("encrypted-string", "string-nonce")
  private val encryptedBigDecimal = EncryptedValue("encrypted-bigdecimal", "bigdecimal-nonce")
  private val encryptedMonth = EncryptedValue("encrypted-month", "month-nonce")
  private val encryptedValue = EncryptedValue("some-value", "some-nonce")

  private implicit val aesGcmAdCrypto: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  "stringCypher" should {
    val stringValue = "some-string-value"
    "encrypt string values" in {
      when(aesGcmAdCrypto.encrypt(eqTo(stringValue))(eqTo(associatedText))).thenReturn(encryptedString)
      stringCypher.encrypt(stringValue) shouldBe encryptedString
    }
    "decrypt to string values" in {
      when(aesGcmAdCrypto.decrypt(eqTo(encryptedValue))(eqTo(associatedText))).thenReturn(stringValue)
      stringCypher.decrypt(encryptedValue) shouldBe stringValue
    }
  }

  "booleanCypher" should {
    val someBoolean = true
    "encrypt boolean values" in {
      when(aesGcmAdCrypto.encrypt(eqTo(someBoolean.toString))(eqTo(associatedText))).thenReturn(encryptedBoolean)
      booleanCypher.encrypt(someBoolean) shouldBe encryptedBoolean
    }
    "decrypt to boolean values" in {
      when(aesGcmAdCrypto.decrypt(eqTo(encryptedValue))(eqTo(associatedText))).thenReturn(someBoolean.toString)
      booleanCypher.decrypt(encryptedValue) shouldBe someBoolean
    }
  }

  "bigDecimalCypher" should {
    val bigDecimalValue: BigDecimal = 500.0
    "encrypt BigDecimal values" in {
      when(aesGcmAdCrypto.encrypt(eqTo(bigDecimalValue.toString))(eqTo(associatedText))).thenReturn(encryptedBigDecimal)
      bigDecimalCypher.encrypt(bigDecimalValue) shouldBe encryptedBigDecimal
    }
    "decrypt to BigDecimal values" in {
      when(aesGcmAdCrypto.decrypt(eqTo(encryptedValue))(eqTo(associatedText))).thenReturn(bigDecimalValue.toString)
      bigDecimalCypher.decrypt(encryptedValue) shouldBe bigDecimalValue
    }
  }

  "monthCypher" should {
    val monthValue: Month = Month.APRIL
    "encrypt Month values" in {
      when(aesGcmAdCrypto.encrypt(eqTo(monthValue.toString))(eqTo(associatedText))).thenReturn(encryptedMonth)
      monthCypher.encrypt(monthValue) shouldBe encryptedMonth
    }
    "decrypt to Month values" in {
      when(aesGcmAdCrypto.decrypt(eqTo(encryptedValue))(eqTo(associatedText))).thenReturn(monthValue.toString)
      monthCypher.decrypt(encryptedValue) shouldBe monthValue
    }
  }
}
