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

import connectors.{IncomeSourceConnector, IncomeTaxUserDataConnector}
import org.scalamock.scalatest.MockFactory
import repositories.StockDividendsUserDataRepository
import utils.IntegrationTest


class StockDividendsSessionServiceISpec extends IntegrationTest with MockFactory {

  val stockDividendsUserDataRepository: StockDividendsUserDataRepository = app.injector.instanceOf[StockDividendsUserDataRepository]
  val getDividendsIncomeService: GetDividendsIncomeService = app.injector.instanceOf[GetDividendsIncomeService]
  val submittedDividendsService: SubmittedDividendsService = mock[SubmittedDividendsService]
  val incomeSourceConnector: IncomeSourceConnector = app.injector.instanceOf[IncomeSourceConnector]
  val incomeTaxUserDataConnector: IncomeTaxUserDataConnector = mock[IncomeTaxUserDataConnector]

  val stockDividendsSessionServiceInvalidEncryption: StockDividendsSessionService =
    appWithInvalidEncryptionKey.injector.instanceOf[StockDividendsSessionService]

  val stockDividendsSessionService: StockDividendsSessionService = new StockDividendsSessionService(
    stockDividendsUserDataRepository,
    incomeSourceConnector
  )

  ".createSessionData" should {
    "return false when failing to decrypt the model" in {
      val result = await(stockDividendsSessionServiceInvalidEncryption.createSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }

    "return true when successful and false when adding a duplicate" in {
      await(stockDividendsUserDataRepository.collection.drop().toFuture())
      await(stockDividendsUserDataRepository.ensureIndexes())
      val initialResult = await(stockDividendsSessionService.createSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      val duplicateResult = await(stockDividendsSessionService.createSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      initialResult shouldBe true
      duplicateResult shouldBe false
    }
  }

  ".updateSessionData" should {
    "return false when failing to decrypt the model" in {
      val result = await(stockDividendsSessionServiceInvalidEncryption.updateSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }

    "create a document when parameter is true" in {
      await(stockDividendsSessionService.clear(taxYear)(false)(true))

      val result = await(
        stockDividendsSessionService.updateSessionData(completeStockDividendsCYAModel, taxYear, needsCreating = true)(false)(true)
      )
      result shouldBe true
    }
  }

  ".getSessionData" should {
    await(stockDividendsSessionService.createSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))

    "return a session model with data" in {
      val result = await(stockDividendsSessionService.getSessionData(taxYear))

      result.map(_.isDefined) shouldBe Right(true)
    }
  }
}
