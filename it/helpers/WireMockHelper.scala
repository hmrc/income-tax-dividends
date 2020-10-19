
package helpers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, delete, equalTo, get, getRequestedFor, patch, post, postRequestedFor, put, stubFor, urlEqualTo, urlMatching, verify}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.stubbing.StubMapping

trait WireMockHelper {

  val wiremockPort = 11111
  val wiremockHost = "localhost"

  lazy val wmConfig: WireMockConfiguration = wireMockConfig().port(wiremockPort)
  lazy val wireMockServer = new WireMockServer(wmConfig)



  def startWiremock(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  def stopWiremock(): Unit = wireMockServer.stop()

  def resetWiremock(): Unit = WireMock.reset()

  def verifyPost(uri: String, optBody: Option[String] = None): Unit = {
    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest = optBody match {
      case Some(body) => uriMapping.withRequestBody(equalTo(body))
      case None => uriMapping
    }
    verify(postRequest)
  }

  def verifyGet(uri: String): Unit = {
    verify(getRequestedFor(urlEqualTo(uri)))
  }

  def stubGet(url: String, status: Integer, body: String): StubMapping =
    stubFor(get(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(body)
      )
    )

  def stubPost(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(post(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubPut(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(put(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubPatch(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(patch(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubDelete(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(delete(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )


}
