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

package repositories

import com.google.inject.ImplementedBy
import config.AppConfig
import models.Done
import models.mongo.JourneyAnswers
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model._
import play.api.Logging
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.play.http.logging.Mdc

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[JourneyAnswersRepositoryImpl])
trait JourneyAnswersRepository {
  def keepAlive(mtdItId: String, taxYear: Int, journey: String): Future[Done]
  def get(mtdItId: String, taxYear: Int, journey: String): Future[Option[JourneyAnswers]]
  def set(userData: JourneyAnswers): Future[Done]
  def clear(mtdItId: String, taxYear: Int, journey: String): Future[Done]
}

@Singleton
class JourneyAnswersRepositoryImpl @Inject()(
                                              mongoComponent: MongoComponent,
                                              appConfig: AppConfig,
                                              clock: Clock
                                            )(implicit ec: ExecutionContext, crypto: Encrypter with Decrypter)
  extends PlayMongoRepository[JourneyAnswers](
    collectionName = "journeyAnswers",
    mongoComponent = mongoComponent,
    domainFormat = JourneyAnswers.encryptedFormat,
    indexes = JourneyAnswersRepositoryIndexes.indexes()(appConfig),
    replaceIndexes = appConfig.replaceJourneyAnswersIndexes
  ) with Logging with JourneyAnswersRepository {

  private def filterByMtdItIdYear(mtdItId: String, taxYear: Int, journey: String): Bson = and(
    equal("mtdItId", toBson(mtdItId)),
    equal("taxYear", toBson(taxYear)),
    equal("journey", toBson(journey))
  )

  def keepAlive(mtdItId: String, taxYear: Int, journey: String): Future[Done] =
    Mdc.preservingMdc {
      collection
        .updateOne(
          filter = filterByMtdItIdYear(mtdItId, taxYear, journey),
          update = Updates.set("lastUpdated", Instant.now(clock))
        )
        .toFuture()
        .map(_ => Done)
    }

  def get(mtdItId: String, taxYear: Int, journey: String): Future[Option[JourneyAnswers]] =
    Mdc.preservingMdc {
      keepAlive(mtdItId, taxYear, journey).flatMap {
        _ =>
          collection
            .find(filterByMtdItIdYear(mtdItId, taxYear, journey))
            .headOption()
      }
    }

  def set(userData: JourneyAnswers): Future[Done] =
    Mdc.preservingMdc {
      collection
        .replaceOne(
          filter = filterByMtdItIdYear(userData.mtdItId, userData.taxYear, userData.journey),
          replacement = userData.copy(lastUpdated = Instant.now(clock)),
          options = ReplaceOptions().upsert(true)
        )
        .toFuture()
        .map(_ => Done)
    }

  def clear(mtdItId: String, taxYear: Int, journey: String): Future[Done] =
    Mdc.preservingMdc {
      collection
        .deleteOne(filterByMtdItIdYear(mtdItId, taxYear, journey))
        .toFuture()
        .map(_ => Done)
    }
}
