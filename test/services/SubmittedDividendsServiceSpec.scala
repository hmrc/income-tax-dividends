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
import connectors.{GetAnnualIncomeSourcePeriodConnector, SubmittedDividendsConnector}
import connectors.httpParsers.SubmittedDividendsHttpParser.SubmittedDividendsResponse
import models.SubmittedDividendsModel
import uk.gov.hmrc.http.HeaderCarrier
import utils.{TaxYearUtils, TestUtils}

import scala.concurrent.Future

class SubmittedDividendsServiceSpec extends TestUtils {
  SharedMetricRegistries.clear()

  val connector: SubmittedDividendsConnector = mock[SubmittedDividendsConnector]
  val ifConnector: GetAnnualIncomeSourcePeriodConnector = mock[GetAnnualIncomeSourcePeriodConnector]
  val service: SubmittedDividendsService = new SubmittedDividendsService(connector, ifConnector)

  private val specificTaxYear: Int = TaxYearUtils.specificTaxYear
  private val specificTaxYearPlusOne: Int = specificTaxYear + 1

  ".getSubmittedDividends" should {

    "return the connector response" in {

      val expectedResult: SubmittedDividendsResponse = Right(SubmittedDividendsModel(Some(20.00), Some(20.00), None))

      (connector.getSubmittedDividends(_: String, _: Int)(_: HeaderCarrier))
        .expects("12345678", taxYear, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.getSubmittedDividends("12345678", taxYear))

      result mustBe expectedResult

    }
  }

  "return the if connector response for specific tax year" in {

    val expectedResult: SubmittedDividendsResponse = Right(SubmittedDividendsModel(Some(20.00), Some(20.00), None))

    (ifConnector.getAnnualIncomeSourcePeriod(_: String, _: Int, _: Option[Boolean])(_: HeaderCarrier))
      .expects("12345678", specificTaxYear, Some(false), *)
      .returning(Future.successful(expectedResult))

    val result = await(service.getSubmittedDividends("12345678", specificTaxYear))

    result mustBe expectedResult
  }

  "return the if connector response for specific tax year plus one" in {

    val expectedResult: SubmittedDividendsResponse = Right(SubmittedDividendsModel(Some(20.00), Some(20.00), None))

    (ifConnector.getAnnualIncomeSourcePeriod(_: String, _: Int, _: Option[Boolean])(_: HeaderCarrier))
      .expects("12345678", specificTaxYearPlusOne, Some(false), *)
      .returning(Future.successful(expectedResult))

    val result = await(service.getSubmittedDividends("12345678", specificTaxYearPlusOne))

    result mustBe expectedResult
  }
}
