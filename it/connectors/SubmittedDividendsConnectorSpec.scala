
package connectors

import connectors.httpParsers.submittedDividendsHttpParser._
import models.SubmittedDividendsModel
import play.api.libs.json.Json
import play.mvc.Http.Status._
import utils.IntegrationTest

class SubmittedDividendsConnectorSpec extends IntegrationTest {

 lazy val connector : SubmittedDividendsConnector = app.injector.instanceOf[SubmittedDividendsConnector]

  val nino: String = "123456789"
  val taxYear: Int = 1234
  val dividends: BigDecimal = 123456.78

  "The submittedDividendsConnector" should {
    "return a submittedDividendsModel" when {
      "all values are present" in {

        val expectedResult = SubmittedDividendsModel(dividends,dividends)

        stubGet(s"/income-tax/nino/$nino/income-source/{incomeSourceType}/annual/$taxYear", OK, Json.toJson(expectedResult).toString)

        val result = await(connector.getSubmittedDividends(nino, taxYear))

        result shouldBe Right(expectedResult)
      }

      "return a SubmittedDividendsInvalidJsonException" in {

        val invalidJson = Json.obj(
          "dividends" -> "",
          "ukDividends" -> ""
        )

        val expectedResult = SubmittedDividendsInvalidJsonException

        stubGet(s"/income-tax/nino/$nino/income-source/{incomeSourceType}/annual/$taxYear", OK, invalidJson.toString())

        val result = await(connector.getSubmittedDividends(nino, taxYear))

        result shouldBe Left(expectedResult)
      }

      "return a SubmittedDividendsNotFoundException" in {

        val expectedResult = SubmittedDividendsNotFoundException

        stubGet(s"/income-tax/nino/$nino/income-source/{incomeSourceType}/annual/$taxYear", NOT_FOUND, "{}")
        val result = await(connector.getSubmittedDividends(nino, taxYear))

        result shouldBe Left(expectedResult)
      }

      "return a SubmittedDividendsServiceUnavailableException" in {

        val expectedResult = SubmittedDividendsServiceUnavailableException

        stubGet(s"/income-tax/nino/$nino/income-source/{incomeSourceType}/annual/$taxYear", SERVICE_UNAVAILABLE, "{}")
        val result = await(connector.getSubmittedDividends(nino, taxYear))

        result shouldBe Left(expectedResult)
      }

      "return a SubmittedDividendsUnhandledException" in {

        val expectedResult = SubmittedDividendsUnhandledException

        stubGet(s"/income-tax/nino/$nino/income-source/{incomeSourceType}/annual/$taxYear", BAD_REQUEST, "{}")
        val result = await(connector.getSubmittedDividends(nino, taxYear))

        result shouldBe Left(expectedResult)
      }
    }
  }


}
