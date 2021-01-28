
package connectors

import helpers.WiremockSpec
import models.{CreateOrAmendDividendsModel, CreateOrAmendDividendsResponseModel, DesErrorBodyModel, DesErrorModel}
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class CreateOrAmendDividendsConnectorSpec extends PlaySpec with WiremockSpec{

  lazy val connector: CreateOrAmendDividendsConnector = app.injector.instanceOf[CreateOrAmendDividendsConnector]

  val nino: String = "123456789"
  val taxYear: Int = 1999
  val updateDividendsModel: CreateOrAmendDividendsModel = CreateOrAmendDividendsModel(Some(123.12), Some(321.21))
  val createOrAmendDividendsResponse = CreateOrAmendDividendsResponseModel("String")
  val dividendResult: Option[BigDecimal] = Some(123456.78)

  "CreateOrAmendDividendsConnector" should {
    "return a success result" when {
      "DES Returns a 200" in {
        val expectedResult = createOrAmendDividendsResponse

        stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, Json.toJson(updateDividendsModel).toString(), Json.toJson(createOrAmendDividendsResponse).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a Parsing error INTERNAL_SERVER_ERROR response" in {
      val invalidJson = Json.obj(
        "ukDividends" -> ""
      )

      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, Json.toJson(updateDividendsModel).toString(), invalidJson.toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Not Found" in {
      val responseBody = Json.obj(
        "code" -> "NOT_FOUND_INCOME_SOURCE",
        "description" -> "Can't find income source"
      )
      val expectedResult = DesErrorModel(404, DesErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source"))

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", NOT_FOUND, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Internal Server Error" in {
      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "description" -> "Internal server error"
      )
      val expectedResult = DesErrorModel(500, DesErrorBodyModel("SERVER_ERROR", "Internal server error"))

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", INTERNAL_SERVER_ERROR, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Service Unavailable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "description" -> "Service is unavailable"
      )
      val expectedResult = DesErrorModel(503, DesErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", SERVICE_UNAVAILABLE, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result with no body" in {
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubPostWithoutResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", NO_CONTENT, Json.toJson(updateDividendsModel).toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result that is parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "description" -> "Service is unavailable"
      )
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR,  DesErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", CONFLICT, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result that isn't parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE"
      )
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR,  DesErrorBodyModel.parsingError)

      stubPostWithResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", CONFLICT, Json.toJson(updateDividendsModel).toString(), responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

  }
}
