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

package services

import connectors.{IncomeSourceConnector, IncomeTaxUserDataConnector}
import repositories.DividendsUserDataRepository
import utils.IntegrationTest


class DividendsSessionServiceISpec extends IntegrationTest {

  val dividendsUserDataRepository: DividendsUserDataRepository = app.injector.instanceOf[DividendsUserDataRepository]
  val incomeTaxUserDataConnector: IncomeTaxUserDataConnector = app.injector.instanceOf[IncomeTaxUserDataConnector]
  val incomeSourceConnector: IncomeSourceConnector = app.injector.instanceOf[IncomeSourceConnector]

  val dividendsSessionServiceInvalidEncryption: DividendsSessionService = appWithInvalidEncryptionKey.injector.instanceOf[DividendsSessionService]
  val dividendsSessionService: DividendsSessionService =
    new DividendsSessionService(incomeSourceConnector, dividendsUserDataRepository)

  ".createSessionData" should {

    "return false when failing to decrypt the model" in {
      val result = await(dividendsSessionServiceInvalidEncryption.createSessionData(completeDividendsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }

    "return true when successful and false when adding a duplicate" in {
      await(dividendsUserDataRepository.collection.drop().toFuture())
      await(dividendsUserDataRepository.ensureIndexes())
      val initialResult = await(dividendsSessionService.createSessionData(completeDividendsCYAModel, taxYear)(false)(true))
      val duplicateResult = await(dividendsSessionService.createSessionData(completeDividendsCYAModel, taxYear)(false)(true))
      initialResult shouldBe true
      duplicateResult shouldBe false
    }
  }

  ".updateSessionData" should{
    "return false when failing to decrypt the model" in {
      val result = await(dividendsSessionServiceInvalidEncryption.updateSessionData(completeDividendsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }

    "create a document when parameter is true" in {
      await(dividendsSessionService.clear(taxYear)(false)(true))

      val result = await(
        dividendsSessionService.updateSessionData(completeDividendsCYAModel, taxYear, needsCreating = true)(false)(true)
      )
      result shouldBe true
    }
  }

  ".getSessionData" should {
    await(dividendsSessionService.createSessionData(completeDividendsCYAModel, taxYear)(false)(true))

    "return a session model with data" in {
      val result = await(dividendsSessionService.getSessionData(taxYear))

      result.map(_.isDefined) shouldBe Right(true)
    }
  }
}
