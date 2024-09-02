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

package connectors

import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import models.dividends.DividendsPriorSubmission
import models.priorDataModels.IncomeSourcesModel
import models.{ErrorBodyModel, ErrorModel, User}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.OK
import uk.gov.hmrc.http.HeaderCarrier
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class IncomeTaxUserDataConnectorSpec extends IntegrationTest {

  lazy val connector: IncomeTaxUserDataConnector = app.injector.instanceOf[IncomeTaxUserDataConnector]

  val testUser: User[AnyContentAsEmpty.type] = User(mtditid, None, nino, sessionId)(FakeRequest())

  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  "IncomeTaxUserDataConnector" should {

    "return a success result" when {

      "submission returns a 204" in {

        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", NO_CONTENT,
          "{}", xSessionId, "mtditid" -> mtditid)

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Right(IncomeSourcesModel())
      }

      "submission returns a 200" in {
        val incomeSourcesModel = IncomeSourcesModel(
          Some(DividendsPriorSubmission(
            Some(199.93), Some(1222342.88)
          ))
        )

        userDataStub(incomeSourcesModel, nino, taxYear)

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Right(incomeSourcesModel)
      }
    }

    "Return an error result" when {

      "submission returns a 200 but invalid json" in {

        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", OK,
          Json.toJson("""{"invalid": true}""").toString(), ("X-Session-ID" -> sessionId), ("mtditid" -> mtditid))

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {

        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", INTERNAL_SERVER_ERROR,
          """{"code": "FAILED", "reason": "failed"}""", ("X-Session-ID" -> sessionId), ("mtditid" -> mtditid))

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {

        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", SERVICE_UNAVAILABLE,
          """{"code": "FAILED", "reason": "failed"}""", ("X-Session-ID" -> sessionId), ("mtditid" -> mtditid))

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(ErrorModel(SERVICE_UNAVAILABLE, ErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {

        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", BAD_REQUEST,
          """{"code": "FAILED", "reason": "failed"}""", ("X-Session-ID" -> sessionId), ("mtditid" -> mtditid))

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(ErrorModel(INTERNAL_SERVER_ERROR, ErrorBodyModel("FAILED", "failed")))
      }
    }
  }

}
