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

package connectors

import config.AppConfig
import models.logging.CorrelationId.CorrelationIdHeaderKey
import uk.gov.hmrc.http.HeaderNames._
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, SessionId}
import utils.TestUtils

import java.util.UUID

class DesConnectorSpec extends TestUtils {

  class FakeConnector(override val appConfig: AppConfig) extends DesConnector {
    def headerCarrierTest(url: String)(hc: HeaderCarrier): HeaderCarrier = desHeaderCarrier(url)(hc)
  }
  val connector = new FakeConnector(mockAppConfig)

  "FakeConnector" when {

    "host is Internal" should {
      val internalHost = "http://localhost"

      "add the correct authorization" in {
        val hc = HeaderCarrier()
        val result = connector.headerCarrierTest(internalHost)(hc)
        result.authorization mustBe Some(Authorization(s"Bearer ${mockAppConfig.authorisationToken}"))
      }

      "add the correct environment" in {
        val hc = HeaderCarrier()
        val result = connector.headerCarrierTest(internalHost)(hc)
        result.extraHeaders mustBe List(
          "Environment" -> mockAppConfig.desEnvironment
        )
      }
    }

    "host is External" should {
      val externalHost = "http://127.0.0.1"

      "include all HeaderCarrier headers in the extraHeaders when the host is external" in {
        val correlationId = UUID.randomUUID().toString
        val hc = HeaderCarrier(sessionId = Some(SessionId("sessionIdHeaderValue")))
        val result = connector.headerCarrierTest(externalHost)(hc.copy(otherHeaders = List(CorrelationIdHeaderKey -> correlationId)))

        result.extraHeaders.size mustBe 5
        result.extraHeaders.contains(xSessionId -> "sessionIdHeaderValue") mustBe true
        result.extraHeaders.contains(authorisation -> s"Bearer ${mockAppConfig.authorisationToken}") mustBe true
        result.extraHeaders.contains("Environment" -> mockAppConfig.desEnvironment) mustBe true
        result.otherHeaders.contains(CorrelationIdHeaderKey -> correlationId) mustBe true
        result.extraHeaders.exists(x => x._1.equalsIgnoreCase(xRequestChain)) mustBe true
      }
    }
  }

}
