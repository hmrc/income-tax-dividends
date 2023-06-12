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
import connectors.{CreateOrAmendDividendsConnector, CreateUpdateAnnualIncomeSourceConnector}
import connectors.httpParsers.CreateOrAmendDividendsHttpParser.CreateOrAmendDividendsResponse
import models.{CreateOrAmendDividendsModel, CreateOrAmendDividendsResponseModel}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{TaxYearUtils, TestUtils}

import scala.concurrent.Future

class CreateOrAmendDividendsServiceSpec extends TestUtils {
  SharedMetricRegistries.clear()

  val connector: CreateOrAmendDividendsConnector = mock[CreateOrAmendDividendsConnector]
  val connector2: CreateUpdateAnnualIncomeSourceConnector = mock[CreateUpdateAnnualIncomeSourceConnector]
  val service: CreateOrAmendDividendsService = new CreateOrAmendDividendsService(connector, connector2)

  private val specificTaxYear: Int = TaxYearUtils.specificTaxYear
  private val specificTaxYearPlusOne: Int = specificTaxYear + 1

  ".createOrAmendDividends" should {

    "return the DES connector response" in {

      val expectedResult: CreateOrAmendDividendsResponse = Right(CreateOrAmendDividendsResponseModel("transactionRef"))

      (connector.createOrAmendDividends(_: String, _: Int, _: CreateOrAmendDividendsModel)(_: HeaderCarrier))
        .expects("12345678", 1234, *, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.createOrAmendDividends("12345678", 1234, CreateOrAmendDividendsModel(Some(12345.66), None)))

      result mustBe expectedResult

    }

    "return the IF connector response for specific tax year" in {

      val expectedResult: CreateOrAmendDividendsResponse = Right(CreateOrAmendDividendsResponseModel("transactionRef"))

      (connector2.createUpdateAnnualIncomeSource(_: String, _: Int, _: CreateOrAmendDividendsModel)(_: HeaderCarrier))
        .expects("12345678", specificTaxYear, *, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.createOrAmendDividends("12345678", specificTaxYear, CreateOrAmendDividendsModel(Some(12345.66), None)))

      result mustBe expectedResult

    }

    "return the IF connector response for specific tax year plus one" in {

      val expectedResult: CreateOrAmendDividendsResponse = Right(CreateOrAmendDividendsResponseModel("transactionRef"))

      (connector2.createUpdateAnnualIncomeSource(_: String, _: Int, _: CreateOrAmendDividendsModel)(_: HeaderCarrier))
        .expects("12345678", specificTaxYearPlusOne, *, *)
        .returning(Future.successful(expectedResult))

      val result = await(service.createOrAmendDividends("12345678", specificTaxYearPlusOne, CreateOrAmendDividendsModel(Some(12345.66), None)))

      result mustBe expectedResult

    }
  }
}
