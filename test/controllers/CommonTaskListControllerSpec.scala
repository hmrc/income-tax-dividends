/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers

import models.taskList.{SectionTitle, TaskListSection}
import org.scalamock.handlers.CallHandler4
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import services.CommonTaskListService
import uk.gov.hmrc.http.HeaderCarrier
import utils.{TaxYearUtils, TestUtils}

import scala.concurrent.{ExecutionContext, Future}

class CommonTaskListControllerSpec extends TestUtils {

  val nino :String = "123456789"
  val mtdItId :String = "1234567890"
  val specificTaxYear: Int = TaxYearUtils.specificTaxYear

  val commonTaskListService: CommonTaskListService = mock[CommonTaskListService]

  val controller = new CommonTaskListController(commonTaskListService, authorisedAction, mockControllerComponents)

  def mockDividendsService(): CallHandler4[Int, String, ExecutionContext, HeaderCarrier, Future[Result]] = {
    (commonTaskListService.get(_: Int, _: String)(_: ExecutionContext, _: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(Ok(Json.toJson(TaskListSection(SectionTitle.DividendsTitle, None)))))
  }

  ".getCommonTaskList" should {

    "return a task list section model" in {

      val result = {
        mockAuth()
        mockDividendsService()
        controller.getCommonTaskList(specificTaxYear, nino)(fakeRequest)
      }

      status(result) mustBe OK
    }
  }
}
