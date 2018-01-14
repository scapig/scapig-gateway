package services

import connectors.ProxyConnector
import models.{ApiIdentifier, ApiRequest, AuthType}
import org.mockito.BDDMockito.given
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import utils.UnitSpec

import scala.concurrent.Future.successful

class ProxyServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  trait Setup {
    val request = FakeRequest("GET", "/hello/world")
    val apiRequest = ApiRequest(
      apiIdentifier = mock[ApiIdentifier],
      serviceBaseUrl = "http://hello-world.service",
      path = "/world",
      authType = AuthType.USER,
      scope = Some("scope"))

    val proxyConnector = mock[ProxyConnector]
    val underTest = new ProxyService(proxyConnector)
  }

  "proxy" should {

    "call and return the response from the microservice" in new Setup {
      val response = Ok("hello")

      given(proxyConnector.proxy(request, apiRequest)).willReturn(successful(response))

      val result = await(underTest.proxy(request, apiRequest))

      result shouldBe response
    }
  }
}
