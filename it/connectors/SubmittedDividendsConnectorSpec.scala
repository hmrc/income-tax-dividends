
package connectors

import connectors.httpParsers.SubmittedDividendsHttpParser._
import helpers.WiremockSpec
import models.SubmittedDividendsModel
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class SubmittedDividendsConnectorSpec extends PlaySpec with WiremockSpec{

  lazy val connector: SubmittedDividendsConnector = app.injector.instanceOf[SubmittedDividendsConnector]

  val nino: String = "123456789"
  val taxYear: Int = 1999
  val dividendResult: Option[BigDecimal] = Some(123456.78)

  ".SubmittedDividendsConnector" should {
    "return a SubmittedDividendsModel" when {
      "all values are present" in {
        val expectedResult = SubmittedDividendsModel(dividendResult, dividendResult)

        stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, Json.toJson(expectedResult).toString())

        implicit val hc = HeaderCarrier()
        val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a SubmittedDividendsInvalidJsonException" in {
      val invalidJson = Json.obj(
        "ukDividends" -> ""
      )

      val expectedResult = SubmittedDividendsInvalidJsonException

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, invalidJson.toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }
    "return a SubmittedDividendsServiceUnavailableException" in {
      val expectedResult = SubmittedDividendsServiceUnavailableException

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", SERVICE_UNAVAILABLE, "{}")
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }
    "return a SubmittedDividendsNotFoundException" in {
      val expectedResult = SubmittedDividendsNotFoundException

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", NOT_FOUND, "{}")
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }
    "return a SubmittedDividendsUnhandledException" in {
      val expectedResult = SubmittedDividendsUnhandledException

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", BAD_REQUEST, "{}")
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

  }
}
