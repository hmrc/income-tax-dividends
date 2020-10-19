
package utils

import config.AppConfig
import helpers.WireMockHelper
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Application, Environment, Mode}
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{Await, Awaitable}
import scala.concurrent.duration.Duration

trait IntegrationTest extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with WireMockHelper with BeforeAndAfterAll{

  implicit val emptyHeaderCarrier: HeaderCarrier = HeaderCarrier()

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  def config: Map[String, String] = Map(
    "auditing.enabled" -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.income-tax-submission.host" -> wiremockHost,
    "microservice.services.income-tax-submission.port" -> wiremockPort.toString
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build

  lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }


}

