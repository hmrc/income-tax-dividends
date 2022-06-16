/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.SubmittedDividendsConnector
import connectors.httpParsers.SubmittedDividendsHttpParser.SubmittedDividendsResponse
import models.SubmittedDividendsModel
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class SubmittedDividendsServiceSpec extends TestUtils {
  SharedMetricRegistries.clear()

  val connector: SubmittedDividendsConnector = mock[SubmittedDividendsConnector]
  val service: SubmittedDividendsService = new SubmittedDividendsService(connector)


  ".getSubmittedDividends" should {

    "return the connector response" in {

      val expectedResult: SubmittedDividendsResponse = Right(SubmittedDividendsModel(Some(20.00), Some(20.00)))

      (connector.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
        .expects("12345678", 1234, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.getSubmittedDividends("12345678", 1234))

      result mustBe expectedResult

    }
  }
}
