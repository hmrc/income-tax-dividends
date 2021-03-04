
package connectors

import helpers.WiremockSpec
import models.{DesErrorBodyModel, DesErrorModel, SubmittedDividendsModel}
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

    "return a Parsing error INTERNAL_SERVER_ERROR response" in {
      val invalidJson = Json.obj(
        "ukDividends" -> ""
      )

      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, invalidJson.toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NO_CONTENT" in {
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", NO_CONTENT, "{}")
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Bad Request" in {
      val responseBody = Json.obj(
        "code" -> "INVALID_NINO",
        "reason" -> "Nino is invalid"
      )
      val expectedResult = DesErrorModel(400, DesErrorBodyModel("INVALID_NINO", "Nino is invalid"))

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", BAD_REQUEST, responseBody.toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Not found" in {
      val responseBody = Json.obj(
        "code" -> "NOT_FOUND_INCOME_SOURCE",
        "reason" -> "Can't find income source"
      )
      val expectedResult = DesErrorModel(404, DesErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source"))

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", NOT_FOUND, responseBody.toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal server error" in {
      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "Internal server error"
      )
      val expectedResult = DesErrorModel(500, DesErrorBodyModel("SERVER_ERROR", "Internal server error"))

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", INTERNAL_SERVER_ERROR, responseBody.toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Service Unavailable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = DesErrorModel(503, DesErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", SERVICE_UNAVAILABLE, responseBody.toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result" in {
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", NO_CONTENT)
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result that is parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR,  DesErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", CONFLICT, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result that isn't parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE"
      )
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR,  DesErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", CONFLICT, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getSubmittedDividends(nino, taxYear)(hc))

      result mustBe Left(expectedResult)
    }
  }
}
