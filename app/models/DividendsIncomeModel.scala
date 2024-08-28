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

import play.api.libs.json.{Json, OFormat}

case class DividendsIncomeDataModel(submittedOn: Option[String],
                                    foreignDividend: Option[Seq[ForeignDividendModel]],
                                    dividendIncomeReceivedWhilstAbroad: Option[Seq[ForeignDividendModel]],
                                    stockDividend: Option[StockDividendModel],
                                    redeemableShares: Option[StockDividendModel],
                                    bonusIssuesOfSecurities: Option[StockDividendModel],
                                    closeCompanyLoansWrittenOff: Option[StockDividendModel])

object DividendsIncomeDataModel {
  implicit val formats: OFormat[DividendsIncomeDataModel] = Json.format[DividendsIncomeDataModel]
}

case class ForeignDividendModel(
                                 countryCode: String,
                                 amountBeforeTax: Option[BigDecimal],
                                 taxTakenOff: Option[BigDecimal],
                                 specialWithholdingTax: Option[BigDecimal],
                                 foreignTaxCreditRelief: Option[Boolean],
                                 taxableAmount: BigDecimal
                               )

object ForeignDividendModel {
  implicit val formats: OFormat[ForeignDividendModel] = Json.format[ForeignDividendModel]
}

case class StockDividendModel(
                               customerReference: Option[String],
                               grossAmount: BigDecimal
                             )

object StockDividendModel {
  implicit val formats: OFormat[StockDividendModel] = Json.format[StockDividendModel]
}
