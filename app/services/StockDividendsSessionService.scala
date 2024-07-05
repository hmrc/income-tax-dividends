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

package services

import connectors.IncomeSourceConnector
import models.{ErrorModel, IncomeSources, User}
import models.dividends.StockDividendsCheckYourAnswersModel
import models.mongo.{DatabaseError, StockDividendsUserDataModel}
import models.priorDataModels.StockDividendsPriorDataModel
import play.api.Logger
import repositories.StockDividendsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StockDividendsSessionService @Inject()(
                                              stockDividendsUserDataRepository: StockDividendsUserDataRepository,
                                              stockDividendsUserDataConnector: GetDividendsIncomeService,
                                              incomeTaxUserDataConnector: SubmittedDividendsService,
                                              incomeSourceConnector: IncomeSourceConnector
                                       ) {

  type StockDividendsPriorDataResponse = Either[ErrorModel, Option[StockDividendsPriorDataModel]]

  lazy val logger: Logger = Logger(this.getClass)

  def getPriorData(taxYear: Int)(implicit user: User[_], ec: ExecutionContext, hc: HeaderCarrier): Future[StockDividendsPriorDataResponse] = {
    incomeTaxUserDataConnector.getSubmittedDividends(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(ukDividends) =>
        stockDividendsUserDataConnector.getDividendsIncomeData(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map{
          case Left(error) => Left(error)
          case Right(stockDividends) =>
            // TODO: add rest of values.... look at this line
            if (ukDividends.ukDividends.isDefined || ukDividends.otherUkDividends.isDefined || stockDividends.stockDividend.isDefined) {
              Right(Some(StockDividendsPriorDataModel.getFromPrior(ukDividends, Some(stockDividends))))
            } else {
              Right(None)
            }        }
    }
  }

  def createSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], ec: ExecutionContext): Future[A] = {

    val userData = StockDividendsUserDataModel(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      Some(cyaModel),
      Instant.now()
    )

    stockDividendsUserDataRepository.create(userData)().map {
      case Right(_) => onSuccess
      case Left(_) => onFail
    }
  }

  def getSessionData(taxYear: Int)(implicit user: User[_], ec: ExecutionContext): Future[Either[DatabaseError, Option[StockDividendsUserDataModel]]] = {

    stockDividendsUserDataRepository.find(taxYear).map {
      case Left(error) =>
        logger.error("[StockDividendsSessionService][getSessionData] Could not find user session.")
        Left(error)
      case Right(userData) => Right(userData)
    }
  }

  def updateSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int, needsCreating: Boolean = false)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], ec: ExecutionContext): Future[A] = {

    val userData = StockDividendsUserDataModel(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      Some(cyaModel),
      Instant.now()
    )

    if (needsCreating) {
      stockDividendsUserDataRepository.create(userData)().map {
        case Right(_) => onSuccess
        case Left(_) => onFail
      }
    } else {
      stockDividendsUserDataRepository.update(userData).map {
        case Right(_) => onSuccess
        case Left(_) => onFail
      }
    }
  }

  def getAndHandle[R](taxYear: Int)(onFail: R)(block: (Option[StockDividendsUserDataModel], Option[StockDividendsPriorDataModel]) => Future[R])
                     (implicit executionContext: ExecutionContext, user: User[_], hc: HeaderCarrier): Future[R] = {
    val result = for {
      optionalCya <- getSessionData(taxYear)
      priorDataResponse <- getPriorData(taxYear)
    } yield {
      priorDataResponse match {
        case Right(prior) => optionalCya match {
          case Left(_) =>  Future(onFail)
          case Right(cyaData) => block(cyaData, prior)
        }
        case Left(_) =>  Future(onFail)
      }
    }
    result.flatten
  }

  def clear[R](taxYear: Int)(onFail: R)(onSuccess: R)(implicit user: User[_], ec: ExecutionContext, hc: HeaderCarrier): Future[R] = {
    incomeSourceConnector.put(taxYear, user.nino, IncomeSources.STOCK_DIVIDENDS)(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(_) => Future.successful(onFail)
      case _ =>
        stockDividendsUserDataRepository.clear(taxYear).map {
          case true => onSuccess
          case false => onFail
        }
    }
  }

}
