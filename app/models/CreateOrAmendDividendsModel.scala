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

import play.api.libs.json._

case class CreateOrAmendDividendsModel(ukDividends: Option[BigDecimal], otherUkDividends: Option[BigDecimal])

object CreateOrAmendDividendsModel {

  implicit val reads: Reads[CreateOrAmendDividendsModel] = (json: JsValue) => {
    (for {
      ukDividends <- (json \ "ukDividends").validateOpt[BigDecimal]
      otherUkDividends <- (json \ "otherUkDividends").validateOpt[BigDecimal]
    } yield CreateOrAmendDividendsModel(ukDividends, otherUkDividends))
      .filter(JsError("One of ukDividends or otherUkDividends required"))(model => model.ukDividends.nonEmpty || model.otherUkDividends.nonEmpty)
  }

  implicit val writes: Writes[CreateOrAmendDividendsModel] = Json.writes[CreateOrAmendDividendsModel]
}
