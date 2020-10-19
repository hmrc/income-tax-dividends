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

import models.SubmittedDividendsModel
import play.api.http.Status.{NOT_FOUND, OK, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object submittedDividendsHttpParser {
  type SubmittedDividendsResponse = Either[SubmittedDividendsException, SubmittedDividendsModel]

  implicit object SubmittedDividendsHttpReads extends HttpReads[SubmittedDividendsResponse] {
    override def read(method: String, url: String, response: HttpResponse): SubmittedDividendsResponse = {
      response.status match {
        case OK => response.json.validate[SubmittedDividendsModel].fold[SubmittedDividendsResponse](
          jsonErrors =>
            Left(SubmittedDividendsInvalidJsonException),
          parsedModel =>
            Right(parsedModel)
        )
        case NOT_FOUND => Left(SubmittedDividendsNotFoundException)
        case SERVICE_UNAVAILABLE => Left(SubmittedDividendsServiceUnavailableException)
        case _ => Left(SubmittedDividendsUnhandledException)
      }
    }
  }


  sealed trait SubmittedDividendsException

  object SubmittedDividendsInvalidJsonException extends SubmittedDividendsException
  object SubmittedDividendsServiceUnavailableException extends SubmittedDividendsException
  object SubmittedDividendsNotFoundException extends SubmittedDividendsException
  object SubmittedDividendsUnhandledException extends SubmittedDividendsException


}
