
package connectors

import connectors.httpParsers.CreateOrAmendDividendsHttpParser.{CreateOrAmendDividendsNotFoundException, CreateOrAmendDividendsServiceUnavailableException, CreateOrAmendDividendsUnhandledException}
import helpers.WiremockSpec
import models.CreateOrAmendDividendsModel
import org.scalatestplus.play.PlaySpec
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

class CreateOrAmendDividendsConnectorSpec extends PlaySpec with WiremockSpec{

  lazy val connector: CreateOrAmendDividendsConnector = app.injector.instanceOf[CreateOrAmendDividendsConnector]

  val nino: String = "123456789"
  val taxYear: Int = 1999
  val updateDividendsModel: CreateOrAmendDividendsModel = CreateOrAmendDividendsModel(Some(123.12), Some(321.21))
  val dividendResult: Option[BigDecimal] = Some(123456.78)

  "CreateOrAmendDividendsConnector" should {
    "return a success result" when {
      "DES Returns a 200" in {
        val expectedResult = true

        stubPostWithoutResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", OK, Json.toJson(updateDividendsModel).toString())

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a CreateOrAmendDividendsServiceUnavailableException" in {
      val expectedResult = CreateOrAmendDividendsServiceUnavailableException

      stubPostWithoutResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", SERVICE_UNAVAILABLE, Json.toJson(updateDividendsModel).toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return a CreateOrAmendDividendsNotFoundException" in {
      val expectedResult = CreateOrAmendDividendsNotFoundException

      stubPostWithoutResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", NOT_FOUND, Json.toJson(updateDividendsModel).toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

    "return a CreateOrAmendDividendsUnhandledException" in {
      val expectedResult = CreateOrAmendDividendsUnhandledException

      stubPostWithoutResponseBody(s"/income-tax/nino/$nino/income-source/dividends/annual/$taxYear", INTERNAL_SERVER_ERROR, Json.toJson(updateDividendsModel).toString())
      implicit val hc = HeaderCarrier()
      val result = await(connector.createOrAmendDividends(nino, taxYear, updateDividendsModel)(hc))

      result mustBe Left(expectedResult)
    }

  }
}
