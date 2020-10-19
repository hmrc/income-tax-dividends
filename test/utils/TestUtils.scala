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

package utils

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.codahale.metrics.SharedMetricRegistries
import config.AppConfig
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, Result}
import play.api.test.{FakeRequest, Helpers}
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

trait TestUtils extends AnyWordSpec with Matchers with MockFactory with GuiceOneAppPerSuite with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    SharedMetricRegistries.clear()
  }

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = ActorMaterializer()

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val fakeRequestWithMtditid: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession("MTDITID" -> "1234567890")
  implicit val emptyHeaderCarrier: HeaderCarrier = HeaderCarrier()

  val mockAppConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val mockControllerComponents: ControllerComponents = Helpers.stubControllerComponents()
  implicit val mockExecutionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit val mockAuthConnector: AuthConnector = mock[AuthConnector]
  implicit val mockAuthService: AuthService = new AuthService(mockAuthConnector)


  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

}

