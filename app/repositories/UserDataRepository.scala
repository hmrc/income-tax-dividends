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

package repositories

import com.mongodb.client.model.ReturnDocument
import models.User
import models.mongo._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions}
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mdc.Mdc
import utils.PagerDutyHelper.PagerDutyKeys.{ENCRYPTION_DECRYPTION_ERROR, FAILED_TO_CREATE_DATA, FAILED_TO_FIND_DATA, FAILED_TO_UPDATE_DATA}
import utils.PagerDutyHelper.pagerDutyLog

import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait UserDataRepository[C <: UserDataTemplate] { self: PlayMongoRepository[C] =>

  val repoName: String

  implicit val ec: ExecutionContext
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  type UserData
  def encryptionMethod: UserData => C
  def decryptionMethod: C => UserData

  def create(userData: UserData)(): Future[Either[DatabaseError, Boolean]] =
    Mdc.preservingMdc {
      withEncryptedData(userData, s"[$repoName][create]") { encryptedData =>
        collection.insertOne(encryptedData).toFuture().map(_ => Right(true)).recover { exception =>
          pagerDutyLog(FAILED_TO_CREATE_DATA, s"[$repoName][create] Failed to create user data. Exception: ${exception.getMessage}")
          Left(DataNotUpdated)
        }
      }
    }

  def find[T](taxYear: Int)(implicit user: User[T]): Future[Either[DatabaseError, Option[UserData]]] =
    Mdc.preservingMdc {
      collection.findOneAndUpdate(
        filter = filter(user.sessionId, user.mtditid, user.nino, taxYear),
        update = set("lastUpdated", toBson(Instant.now(Clock.systemUTC()))),
        options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
      ).toFutureOption().map { data =>
        Try(data.map(decryptionMethod)).fold(
          handleEncryptionDecryptionException(_, s"[$repoName][find]"),
          Right(_)
        )
      }.recover { exception =>
        pagerDutyLog(FAILED_TO_FIND_DATA, s"[$repoName][find] Failed when trying to find user data. Exception: ${exception.getMessage}")
        Left(DataNotFound)
      }
    }

  def update(userData: UserData): Future[Either[DatabaseError, Boolean]] =
    Mdc.preservingMdc {
      withEncryptedData(userData, s"[$repoName][update]") { encryptedData =>
        collection.findOneAndReplace(
          filter = filter(encryptedData.sessionId, encryptedData.mtdItId, encryptedData.nino, encryptedData.taxYear),
          replacement = encryptedData,
          options = FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER)
        ).toFutureOption().map {
          case Some(_) => Right(true)
          case _ =>
            pagerDutyLog(FAILED_TO_UPDATE_DATA, s"[$repoName][update] Failed to update user data as no data was found")
            Left(DataNotUpdated)
        }.recover { exception =>
          pagerDutyLog(FAILED_TO_UPDATE_DATA, s"[$repoName][update] Failed to update user data. Exception from Mongo: ${exception.getMessage}")
          Left(DataNotUpdated)
        }
      }
    }

  def clear(taxYear: Int)(implicit user: User[_]): Future[Boolean] =
    Mdc.preservingMdc {
      collection.deleteOne(
        filter = filter(user.sessionId, user.mtditid, user.nino, taxYear)
      ).toFuture().map(_.getDeletedCount == 1)
    }

  def filter(sessionId: String, mtdItId: String, nino: String, taxYear: Int): Bson = and(
    equal("sessionId", toBson(sessionId)),
    equal("mtdItId", toBson(mtdItId)),
    equal("nino", toBson(nino)),
    equal("taxYear", toBson(taxYear))
  )

  private def withEncryptedData[T](userData: UserData, logPrefix: String)
                                  (block: C => Future[Either[DatabaseError, T]]): Future[Either[DatabaseError, T]] =
    Try(encryptionMethod(userData)).fold(
      e => Future.successful(handleEncryptionDecryptionException(e, logPrefix)),
      block
    )

  private def handleEncryptionDecryptionException[T](exception: Throwable, startOfMessage: String): Left[DatabaseError, T] = {
    pagerDutyLog(ENCRYPTION_DECRYPTION_ERROR, s"$startOfMessage ${exception.getMessage}")
    Left(EncryptionDecryptionError(exception.getMessage))
  }
}
