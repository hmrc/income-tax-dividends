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

package connectors.httpParsers

import models.{ErrorBodyModel, ErrorModel, ErrorsBodyModel}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HttpResponse
import utils.PagerDutyHelper.PagerDutyKeys.{BAD_SUCCESS_JSON_FROM_API, UNEXPECTED_RESPONSE_FROM_API}
import utils.PagerDutyHelper.{getCorrelationId, pagerDutyLog}

trait APIParser {

  val parserName : String
  val isDes: Boolean = true

  def logMessage(response:HttpResponse): String ={
    s"[$parserName][read] Received ${response.status} from ${if (isDes) "DES" else "IF"}. Body:${response.body}" + getCorrelationId(response)
  }

  def badSuccessJsonFromAPI[Response]: Either[ErrorModel, Response] = {
    pagerDutyLog(BAD_SUCCESS_JSON_FROM_API, s"[$parserName][read] Invalid Json from ${if (isDes) "DES" else "IF"}.")
    Left(ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError))
  }

  def handleAPIError[Response](response: HttpResponse, statusOverride: Option[Int] = None): Either[ErrorModel, Response] = {

    val status = statusOverride.getOrElse(response.status)

    try {
      val json = response.json

      lazy val desError = json.asOpt[ErrorBodyModel]
      lazy val desErrors = json.asOpt[ErrorsBodyModel]

      (desError, desErrors) match {
        case (Some(desError), _) => Left(ErrorModel(status, desError))
        case (_, Some(desErrors)) => Left(ErrorModel(status, desErrors))
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, s"[$parserName][read] Unexpected Json from ${if (isDes) "DES" else "IF"}.")
          Left(ErrorModel(status, ErrorBodyModel.parsingError))
      }
    } catch {
      case _: Exception => Left(ErrorModel(status, ErrorBodyModel.parsingError))
    }
  }
}
