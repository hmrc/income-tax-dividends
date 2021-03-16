/*
 * Copyright 2021 HM Revenue & Customs
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

import models.{CreateOrAmendDividendsResponseModel, DesErrorBodyModel, DesErrorModel}
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, OK, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys.{BAD_SUCCESS_JSON_FROM_DES, FOURXX_RESPONSE_FROM_DES, INTERNAL_SERVER_ERROR_FROM_DES, SERVICE_UNAVAILABLE_FROM_DES, UNEXPECTED_RESPONSE_FROM_DES}
import utils.PagerDutyHelper.{getCorrelationId, pagerDutyLog}

object CreateOrAmendDividendsHttpParser {
  type CreateOrAmendDividendsResponse = Either[DesErrorModel, CreateOrAmendDividendsResponseModel]

  implicit object CreateOrAmendDividendsHttpReads extends HttpReads[CreateOrAmendDividendsResponse] {
    override def read(method: String, url: String, response: HttpResponse): CreateOrAmendDividendsResponse = {
      response.status match {
        case OK =>
          response.json.validate[CreateOrAmendDividendsResponseModel].fold[CreateOrAmendDividendsResponse](
            jsonErrors => {
              pagerDutyLog(BAD_SUCCESS_JSON_FROM_DES, Some(s"[CreateOrAmendDividendsHttpParser][read] Invalid Json from DES."))
              Left(DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError))
            },
            parsedModel => Right(parsedModel)
          )
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_DES, logMessage(response))
          handleDESError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_DES, logMessage(response))
          handleDESError(response)
        case BAD_REQUEST | FORBIDDEN =>
          pagerDutyLog(FOURXX_RESPONSE_FROM_DES, logMessage(response))
          handleDESError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_DES, logMessage(response))
          handleDESError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }

  private def logMessage(response:HttpResponse): Option[String] ={
    Some(s"[CreateOrAmendDividendsHttpParser][read] Received ${response.status} from DES. Body:${response.body}" + getCorrelationId(response))
  }

  private def handleDESError(response: HttpResponse, statusOverride: Option[Int] = None):CreateOrAmendDividendsResponse = {
    val status = statusOverride.getOrElse(response.status)

    try {
      response.json.validate[DesErrorBodyModel].fold[CreateOrAmendDividendsResponse](
        jsonErrors => {
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_DES, Some(s"[CreateOrAmendDividendsHttpParser][read] Unexpected Json from DES."))
          Left(DesErrorModel(status, DesErrorBodyModel.parsingError))
        },
        parsedError => Left(DesErrorModel(status, parsedError))
      )
    } catch {
      case _: Exception => Left(DesErrorModel(status, DesErrorBodyModel.parsingError))
    }
  }

}
