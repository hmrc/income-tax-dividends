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

package connectors.httpParsers

import models.{CreateOrAmendDividendsResponseModel, ErrorModel}
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SERVICE_UNAVAILABLE, UNPROCESSABLE_ENTITY}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys.{FOURXX_RESPONSE_FROM_API, INTERNAL_SERVER_ERROR_FROM_API, SERVICE_UNAVAILABLE_FROM_API}
import utils.PagerDutyHelper.pagerDutyLog

object CreateUpdateAnnualIncomeSourceHttpParser extends APIParser with Logging {

  type CreateUpdateAnnualIncomeSourceResponse = Either[ErrorModel, CreateOrAmendDividendsResponseModel]

  implicit object CreateUpdateAnnualIncomeSourceHttpReads extends HttpReads[CreateUpdateAnnualIncomeSourceResponse] {
    override def read(method: String, url: String, response: HttpResponse): CreateUpdateAnnualIncomeSourceResponse = {
      response.status match {
        case OK =>
          response.json.validate[CreateOrAmendDividendsResponseModel].fold[CreateUpdateAnnualIncomeSourceResponse](
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
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleAPIError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
}
