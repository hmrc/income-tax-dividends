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

package helpers

import com.codahale.metrics.SharedMetricRegistries
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.ExecutionContext

trait WiremockSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with GuiceOneServerPerSuite
  with FutureAwaits with DefaultAwaitTimeout with WiremockStubHelpers {

  val wireMockPort = 11111

  lazy val ws: WSClient = app.injector.instanceOf(classOf[WSClient])
  implicit val ec: ExecutionContext = ExecutionContext.global

  val wireMockServer: WireMockServer = new WireMockServer(wireMockConfig().port(wireMockPort))

  lazy val connectedServices: Seq[String] = Seq("des", "integration-framework")

  def servicesToUrlConfig: Seq[(String, String)] = connectedServices
    .flatMap(service => Seq(s"microservice.services.$service.host" -> s"localhost", s"microservice.services.$service.port" -> wireMockPort.toString))

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .configure(
      ("auditing.enabled" -> false) +:
        servicesToUrlConfig: _*
    )
    .build()

  protected override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
    SharedMetricRegistries.clear()
    WireMock.configureFor("localhost", wireMockPort)
  }

  protected override def afterAll(): Unit = {
    super.afterAll()
    wireMockServer.stop()
  }

  protected override def beforeEach(): Unit = {
    super.beforeEach()
    reset()
  }

  def buildClient(urlandUri: String, port: Int = port): WSRequest = ws
    .url(s"http://localhost:$port$urlandUri")
    .withFollowRedirects(false)

}
