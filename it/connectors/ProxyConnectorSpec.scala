package connectors

import java.util.concurrent.TimeoutException

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import config.AppContext
import models._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest
import utils.UnitSpec

class ProxyConnectorSpec extends UnitSpec with BeforeAndAfterAll with BeforeAndAfterEach with MockitoSugar {
  val port = 7001

  val wireMockServer = new WireMockServer(wireMockConfig().port(port))
  val playApplication = new GuiceApplicationBuilder().build()

  val request = FakeRequest("GET", "/hello/world")
  val apiRequest = ApiRequest(
    apiIdentifier = ApiIdentifier("c", "v"),
    serviceBaseUrl = s"http://localhost:$port",
    path = "/world",
    authType = AuthType.USER,
    clientId = Some("clientId"))

  override def beforeAll {
    configureFor(port)
    wireMockServer.start()
  }

  override def afterAll: Unit = {
    wireMockServer.stop()
  }

  override def beforeEach(): Unit = {
    WireMock.reset()
  }

  trait Setup {
    val appContext = mock[AppContext]
    val underTest = new ProxyConnector(appContext, playApplication.injector.instanceOf[WSClient])
    when(appContext.requestTimeoutInMilliseconds).thenReturn(50)
  }

  "proxy" should {
    "proxy the request when the downstream service response is processed on time" in new Setup {
      when(appContext.requestTimeoutInMilliseconds).thenReturn(500)

      givenGetReturns("/world", OK, delay = 10)

      val result = await(underTest.proxy(request, apiRequest))

      status(result) shouldBe OK
    }

    "proxy the request to the sandbox endpoint when the request is sandbox" in new Setup {
      val sandboxRequest = apiRequest.copy(environment = Some(Environment.SANDBOX))

      givenGetReturns("/sandbox/world", Status.NO_CONTENT)

      val result = await(underTest.proxy(request, sandboxRequest))

      status(result) shouldBe Status.NO_CONTENT
    }

    "proxy the body" in new Setup {
      val body = """{"content":"body"}"""
      val requestWithBody = FakeRequest("POST", "/hello/world").withBody(AnyContentAsJson(Json.parse(body)))

      givenPostReturns("/world", OK)

      await(underTest.proxy(requestWithBody, apiRequest))

      verify(postRequestedFor(urlEqualTo("/world")).withRequestBody(equalTo(body)))
    }

    "forward the headers to the microservice" in new Setup {
      val requestWithHeader = request.withHeaders("aHeader" -> "aHeaderValue")

      givenGetReturns("/world", OK)

      await(underTest.proxy(requestWithHeader, apiRequest))

      verify(getRequestedFor(urlEqualTo("/world"))
        .withHeader("aHeader", equalTo("aHeaderValue")))
    }

    "not forward the Host header from the original request to the microservice" in new Setup {
      val requestWithHeader = request.withHeaders("Host" -> "nginx.service")

      givenGetReturns("/world", OK)

      await(underTest.proxy(requestWithHeader, apiRequest))

      verify(getRequestedFor(urlEqualTo("/world")).withHeader("Host", equalTo(s"localhost:$port")))
    }

    val gatewayHeaders = Map(
      "X-Client-ID" -> apiRequest.clientId.get,
      "X-Request-ID" -> apiRequest.requestId.toString)

    "Add extra headers in the request" in new Setup {

      for ((header, value) <- gatewayHeaders) {

        givenGetReturns("/world", OK)

        await(underTest.proxy(request, apiRequest))

        verify(getRequestedFor(urlEqualTo("/world"))
          .withHeader(header, equalTo(value)))
      }
    }

    "override the extra headers from the original request" in new Setup {

      for ((header, value) <- gatewayHeaders) {

        val requestWithHeader = request.withHeaders(header -> "originalRequestHeader")

        givenGetReturns("/world", OK)

        await(underTest.proxy(requestWithHeader, apiRequest))

        verify(getRequestedFor(urlEqualTo("/world")).withHeader(header, equalTo(value)))
      }
    }

    "fail with a `TimeoutException` when the downstream service response is too slow" in new Setup {

      when(appContext.requestTimeoutInMilliseconds).thenReturn(10)

      givenGetReturns("/world", OK, delay =  100)

      intercept[TimeoutException] {
        await(underTest.proxy(request, apiRequest))
      }
    }

  }

  private def givenGetReturns(endpoint: String, status: Int, delay: Int = 0) = {
    stubFor(
      get(urlEqualTo(endpoint))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withFixedDelay(delay)
        )
    )
  }

  private def givenPostReturns(endpoint: String, status: Int, delay: Int = 0) = {
    stubFor(
      post(urlEqualTo(endpoint))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withFixedDelay(delay)
        )
    )
  }
}
