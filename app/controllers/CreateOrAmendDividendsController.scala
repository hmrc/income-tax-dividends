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

package controllers

import connectors.httpParsers.CreateOrAmendDividendsHttpParser.CreateOrAmendDividendsResponse
import controllers.predicates.AuthorisedAction
import javax.inject.Inject
import models.CreateOrAmendDividendsModel
import play.api.libs.json.JsSuccess
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.CreateOrAmendDividendsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}

class CreateOrAmendDividendsController @Inject()(createOrAmendDividendsService: CreateOrAmendDividendsService,
                                             cc: ControllerComponents,
                                             authorisedAction: AuthorisedAction)
                                            (implicit ec: ExecutionContext) extends BackendController(cc){

  def createOrAmendDividends(nino: String, taxYear:Int, mtditid: String): Action[AnyContent] = authorisedAction.async(mtditid) { implicit user =>
    user.request.body.asJson.map(_.validate[CreateOrAmendDividendsModel]) match {
      case Some(JsSuccess(model, _)) => responseHandler(createOrAmendDividendsService.createOrAmendDividends(nino, taxYear, model))
      case _ => Future.successful(BadRequest)
    }
  }

  def responseHandler(serviceResponse: Future[CreateOrAmendDividendsResponse]): Future[Result] ={
    serviceResponse.map {
      case Right(responseModel) => NoContent
      case Left(error) => InternalServerError
    }
  }
}
