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

import models.{CreateOrAmendDividendsResponseModel, ErrorModel}
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.pagerDutyLog
import utils.PagerDutyHelper.PagerDutyKeys._

object CreateOrAmendDividendsHttpParser extends APIParser {
  type CreateOrAmendDividendsResponse = Either[ErrorModel, CreateOrAmendDividendsResponseModel]

  override val parserName: String = "CreateOrAmendDividendsHttpParser"

  implicit object CreateOrAmendDividendsHttpReads extends HttpReads[CreateOrAmendDividendsResponse] {
    override def read(method: String, url: String, response: HttpResponse): CreateOrAmendDividendsResponse = {
      response.status match {
        case OK =>
          response.json.validate[CreateOrAmendDividendsResponseModel].fold[CreateOrAmendDividendsResponse](
            jsonErrors => badSuccessJsonFromAPI,
            parsedModel => Right(parsedModel)
          )
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleAPIError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleAPIError(response)
        case BAD_REQUEST | FORBIDDEN | NOT_FOUND | UNPROCESSABLE_ENTITY =>
          pagerDutyLog(FOURXX_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
