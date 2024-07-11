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

import connectors.IncomeSourceConnector
import repositories.StockDividendsUserDataRepository
import utils.IntegrationTest


class StockDividendsSessionServiceISpec extends IntegrationTest{

  val stockDividendsUserDataRepository: StockDividendsUserDataRepository = app.injector.instanceOf[StockDividendsUserDataRepository]
  val getDividendsIncomeService: GetDividendsIncomeService = app.injector.instanceOf[GetDividendsIncomeService]
  val submittedDividendsService: SubmittedDividendsService = app.injector.instanceOf[SubmittedDividendsService]
  val incomeSourceConnector: IncomeSourceConnector = app.injector.instanceOf[IncomeSourceConnector]

  val stockDividendsSessionServiceInvalidEncryption: StockDividendsSessionService =
    appWithInvalidEncryptionKey.injector.instanceOf[StockDividendsSessionService]

  //same error in personal
  val stockDividendsSessionService: StockDividendsSessionService = new StockDividendsSessionService(
    stockDividendsUserDataRepository,
    getDividendsIncomeService,
    submittedDividendsService,
    incomeSourceConnector
  )

  "create" should{
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

  "update" should{
    "return false when failing to decrypt the model" in {
      val result = await(stockDividendsSessionServiceInvalidEncryption.updateSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }
  }

}