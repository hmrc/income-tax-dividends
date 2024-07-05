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

package controllers

import controllers.predicates.AuthorisedAction
import models.dividends.StockDividendsCheckYourAnswersModel
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.{GetDividendsIncomeService, StockDividendsSessionService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

// TODO: look at naming
class GetStockDividendsIncomeController @Inject()(getDividendsIncomeDataService: StockDividendsSessionService,
                                                  cc: ControllerComponents,
                                                  authorisedAction: AuthorisedAction)
                                                 (implicit ec: ExecutionContext) extends BackendController(cc) {

  def get(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    getDividendsIncomeDataService.getSessionData(taxYear).map {
      case Right(getDividendsIncomeDataModel) => Ok(Json.toJson(getDividendsIncomeDataModel))
      case Left(errorModel) =>
        //TODO: fix this
        Status(404)(errorModel.message)
    }
  }
  def create(taxYear: Int): Action[AnyContent] =  authorisedAction.async { implicit user =>
    //TODO: don't use .get after validate, pull into separate variable > check responses
    getDividendsIncomeDataService.createSessionData(user.body.asJson.get.validate[StockDividendsCheckYourAnswersModel].get, taxYear)(NotModified)(NoContent)
  }

  //TODO: don't use .get after validate, pull into separate variable > check responses
  def update(taxYear: Int): Action[AnyContent] =  authorisedAction.async { implicit user =>
    getDividendsIncomeDataService.updateSessionData(user.body.asJson.get.validate[StockDividendsCheckYourAnswersModel].get, taxYear)(NotModified)(NoContent)
  }
}
