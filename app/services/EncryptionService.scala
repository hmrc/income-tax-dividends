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

import models.dividends.{DividendsCheckYourAnswersModel, EncryptedDividendsCheckYourAnswersModel, EncryptedStockDividendsCheckYourAnswersModel, StockDividendsCheckYourAnswersModel}
import models.mongo._
import utils.AesGcmAdCrypto
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}

import javax.inject.Inject

class EncryptionService @Inject()(implicit encryptionService: AesGcmAdCrypto) {

  // DIVIDENDS
  def encryptDividendsUserData(dividendsUserDataModel: DividendsUserDataModel): EncryptedDividendsUserDataModel = {
    implicit val associatedText: String = dividendsUserDataModel.mtdItId

    EncryptedDividendsUserDataModel(
      sessionId = dividendsUserDataModel.sessionId,
      mtdItId = dividendsUserDataModel.mtdItId,
      nino = dividendsUserDataModel.nino,
      taxYear = dividendsUserDataModel.taxYear,
      dividends = dividendsUserDataModel.dividends.map(encryptDividendsCheckYourAnswersModel),
      lastUpdated = dividendsUserDataModel.lastUpdated
    )
  }

  def decryptDividendsUserData(encryptedDividendsUserDataModel: EncryptedDividendsUserDataModel): DividendsUserDataModel = {
    implicit val associatedText: String = encryptedDividendsUserDataModel.mtdItId

    DividendsUserDataModel(
      sessionId = encryptedDividendsUserDataModel.sessionId,
      mtdItId = encryptedDividendsUserDataModel.mtdItId,
      nino = encryptedDividendsUserDataModel.nino,
      taxYear = encryptedDividendsUserDataModel.taxYear,
      dividends = encryptedDividendsUserDataModel.dividends.map(decryptDividendsCheckYourAnswersModel),
      lastUpdated = encryptedDividendsUserDataModel.lastUpdated
    )
  }

  private def encryptDividendsCheckYourAnswersModel(dividends: DividendsCheckYourAnswersModel)
                                                   (implicit associatedText: String): EncryptedDividendsCheckYourAnswersModel = {
    EncryptedDividendsCheckYourAnswersModel(
      dividends.gateway.map(_.encrypted),
      dividends.ukDividends.map(_.encrypted),
      dividends.ukDividendsAmount.map(_.encrypted),
      dividends.otherUkDividends.map(_.encrypted),
      dividends.otherUkDividendsAmount.map(_.encrypted)
    )
  }

  private def decryptDividendsCheckYourAnswersModel(dividends: EncryptedDividendsCheckYourAnswersModel)
                                                   (implicit associatedText: String): DividendsCheckYourAnswersModel = {
    DividendsCheckYourAnswersModel(
      dividends.gateway.map(_.decrypted[Boolean]),
      dividends.ukDividends.map(_.decrypted[Boolean]),
      dividends.ukDividendsAmount.map(_.decrypted[BigDecimal]),
      dividends.otherUkDividends.map(_.decrypted[Boolean]),
      dividends.otherUkDividendsAmount.map(_.decrypted[BigDecimal])
    )
  }

  //StockDividends
  def encryptStockDividendsUserData(stockDividendsUserDataModel: StockDividendsUserDataModel): EncryptedStockDividendsUserDataModel = {
    implicit val associatedText: String = stockDividendsUserDataModel.mtdItId

    EncryptedStockDividendsUserDataModel(
      sessionId = stockDividendsUserDataModel.sessionId,
      mtdItId = stockDividendsUserDataModel.mtdItId,
      nino = stockDividendsUserDataModel.nino,
      taxYear = stockDividendsUserDataModel.taxYear,
      stockDividends = stockDividendsUserDataModel.stockDividends.map(encryptStockDividendsCheckYourAnswersModel),
      lastUpdated = stockDividendsUserDataModel.lastUpdated
    )
  }

  def decryptStockDividendsUserData(encryptedStockDividendsUserDataModel: EncryptedStockDividendsUserDataModel): StockDividendsUserDataModel = {
    implicit val associatedText: String = encryptedStockDividendsUserDataModel.mtdItId

    StockDividendsUserDataModel(
      sessionId = encryptedStockDividendsUserDataModel.sessionId,
      mtdItId = encryptedStockDividendsUserDataModel.mtdItId,
      nino = encryptedStockDividendsUserDataModel.nino,
      taxYear = encryptedStockDividendsUserDataModel.taxYear,
      stockDividends = encryptedStockDividendsUserDataModel.stockDividends.map(decryptStockDividendsCheckYourAnswersModel),
      lastUpdated = encryptedStockDividendsUserDataModel.lastUpdated
    )
  }

  private def encryptStockDividendsCheckYourAnswersModel(stockDividends: StockDividendsCheckYourAnswersModel)
                                                        (implicit associatedText: String): EncryptedStockDividendsCheckYourAnswersModel = {
    EncryptedStockDividendsCheckYourAnswersModel(
      stockDividends.gateway.map(_.encrypted),
      stockDividends.ukDividends.map(_.encrypted),
      stockDividends.ukDividendsAmount.map(_.encrypted),
      stockDividends.otherUkDividends.map(_.encrypted),
      stockDividends.otherUkDividendsAmount.map(_.encrypted),
      stockDividends.stockDividends.map(_.encrypted),
      stockDividends.stockDividendsAmount.map(_.encrypted),
      stockDividends.redeemableShares.map(_.encrypted),
      stockDividends.redeemableSharesAmount.map(_.encrypted),
      stockDividends.closeCompanyLoansWrittenOff.map(_.encrypted),
      stockDividends.closeCompanyLoansWrittenOffAmount.map(_.encrypted)
    )
  }

  private def decryptStockDividendsCheckYourAnswersModel(stockDividends: EncryptedStockDividendsCheckYourAnswersModel)
                                                        (implicit associatedText: String): StockDividendsCheckYourAnswersModel = {
    StockDividendsCheckYourAnswersModel(
      stockDividends.gateway.map(_.decrypted[Boolean]),
      stockDividends.ukDividends.map(_.decrypted[Boolean]),
      stockDividends.ukDividendsAmount.map(_.decrypted[BigDecimal]),
      stockDividends.otherUkDividends.map(_.decrypted[Boolean]),
      stockDividends.otherUkDividendsAmount.map(_.decrypted[BigDecimal]),
      stockDividends.stockDividends.map(_.decrypted[Boolean]),
      stockDividends.stockDividendsAmount.map(_.decrypted[BigDecimal]),
      stockDividends.redeemableShares.map(_.decrypted[Boolean]),
      stockDividends.redeemableSharesAmount.map(_.decrypted[BigDecimal]),
      stockDividends.closeCompanyLoansWrittenOff.map(_.decrypted[Boolean]),
      stockDividends.closeCompanyLoansWrittenOffAmount.map(_.decrypted[BigDecimal])
    )
  }
}
