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

import models.{DesErrorBodyModel, DesErrorModel, SubmittedDividendsModel}
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object SubmittedDividendsHttpParser {
  type SubmittedDividendsResponse = Either[DesErrorModel, SubmittedDividendsModel]

  implicit object SubmittedDividendsHttpReads extends HttpReads[SubmittedDividendsResponse] {
    override def read(method: String, url: String, response: HttpResponse): SubmittedDividendsResponse = {
      response.status match {
        case OK => response.json.validate[SubmittedDividendsModel].fold[SubmittedDividendsResponse](
          jsonErrors =>
            Left(DesErrorModel(OK, DesErrorBodyModel.parsingError)),
          parsedModel =>
            Right(parsedModel)
        )
        case _ => response.json.validate[DesErrorBodyModel].fold[SubmittedDividendsResponse](
          jsonErrors =>
            Left(DesErrorModel(response.status, DesErrorBodyModel.parsingError)),
          parsedError =>
            Left(DesErrorModel(response.status, parsedError))
        )
      }
    }
  }

}
