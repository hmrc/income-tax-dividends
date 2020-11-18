/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.http.Status.{NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object CreateOrAmendDividendsHttpParser {
  type CreateOrAmendDividendsResponse = Either[CreateOrAmendDividendsException, Boolean]

  implicit object CreateOrAmendDividendsHttpReads extends HttpReads[CreateOrAmendDividendsResponse] {
    override def read(method: String, url: String, response: HttpResponse): CreateOrAmendDividendsResponse = {
      response.status match {
        case OK => Right(true)
        case NOT_FOUND => Left(CreateOrAmendDividendsNotFoundException)
        case SERVICE_UNAVAILABLE => Left(CreateOrAmendDividendsServiceUnavailableException)
        case _ => Left(CreateOrAmendDividendsUnhandledException)
      }
    }
  }


  sealed trait CreateOrAmendDividendsException

  object CreateOrAmendDividendsServiceUnavailableException extends CreateOrAmendDividendsException
  object CreateOrAmendDividendsNotFoundException extends CreateOrAmendDividendsException
  object CreateOrAmendDividendsUnhandledException extends CreateOrAmendDividendsException


}
