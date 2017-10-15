package services.routing

import models.GatewayError.{MatchingResourceNotFound, ServerError}
import models.{ApiIdentifier, ApiRequest, AuthType, ProxyRequest}
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import utils.UnitSpec

import scala.concurrent.Future.{failed, successful}

class RoutingServiceSpec extends UnitSpec with MockitoSugar {

  private val openRequest = FakeRequest("GET", "/hello/world")
  private val openApiRequest = ApiRequest(
    apiIdentifier = ApiIdentifier("foo1", "1.0"),
    authType = AuthType.NONE,
    apiEndpoint = "http://api.service1//hello/world")

  private val userRequest = FakeRequest("GET", "/hello/user")
  private val userApiRequest = ApiRequest(
    apiIdentifier = ApiIdentifier("foo2", "2.0"),
    authType = AuthType.USER,
    apiEndpoint = "http://api.service2//hello/user",
    scope = Some("scope2"))

  private val applicationRequest = FakeRequest("GET", "/hello/application")
  private val applicationApiRequest = ApiRequest(
    apiIdentifier = ApiIdentifier("foo3", "3.0"),
    authType = AuthType.APPLICATION,
    apiEndpoint = "http://api.service3//hello/application")

  private trait Setup {
    val endpointService = mock[EndpointService]
    val userRestrictedEndpointService = mock[UserRestrictedEndpointService]
    val applicationRestrictedEndpointService = mock[ApplicationRestrictedEndpointService]

    val routingService = new RoutingService(endpointService, userRestrictedEndpointService, applicationRestrictedEndpointService)
  }

  "routeRequest" should {

    "decline an open-endpoint request which fails endpoint match filter" in new Setup {
      when(endpointService.apiRequest(any[ProxyRequest], any[Request[AnyContent]])).thenReturn(failed(MatchingResourceNotFound()))

      intercept[MatchingResourceNotFound] {
        await(routingService.routeRequest(openRequest))
      }
    }

    "route an open-endpoint request" in new Setup {
      when(endpointService.apiRequest(any[ProxyRequest], any[Request[AnyContent]])).thenReturn(successful(openApiRequest))

      val apiRequest = await(routingService.routeRequest(openRequest))

      apiRequest shouldBe openApiRequest
    }

    "decline a user-endpoint request which fails to route" in new Setup {
      when(endpointService.apiRequest(any[ProxyRequest], any[Request[AnyContent]])).thenReturn(successful(userApiRequest))
      when(userRestrictedEndpointService.routeRequest(userRequest, ProxyRequest(userRequest), userApiRequest))
        .thenReturn(failed(ServerError()))

      intercept[ServerError] {
        await(routingService.routeRequest(userRequest))
      }
    }

    "route a user-endpoint request" in new Setup {
      when(endpointService.apiRequest(any[ProxyRequest], any[Request[AnyContent]])).thenReturn(successful(userApiRequest))
      when(userRestrictedEndpointService.routeRequest(userRequest, ProxyRequest(userRequest), userApiRequest))
        .thenReturn(successful(userApiRequest))

      val apiRequest = await(routingService.routeRequest(userRequest))

      apiRequest shouldBe userApiRequest
    }

    "decline an application-endpoint request which fails to route" in new Setup {
      when(endpointService.apiRequest(any[ProxyRequest], any[Request[AnyContent]])).thenReturn(successful(applicationApiRequest))
      when(applicationRestrictedEndpointService.routeRequest(applicationRequest, ProxyRequest(applicationRequest), applicationApiRequest))
        .thenReturn(failed(ServerError()))

      intercept[ServerError] {
        await(routingService.routeRequest(applicationRequest))
      }
    }

    "route an application-endpoint request" in new Setup {
      when(endpointService.apiRequest(any[ProxyRequest], any[Request[AnyContent]])).thenReturn(successful(applicationApiRequest))
      when(applicationRestrictedEndpointService.routeRequest(applicationRequest, ProxyRequest(applicationRequest), applicationApiRequest))
        .thenReturn(successful(applicationApiRequest))

      val apiRequest = await(routingService.routeRequest(applicationRequest))

      apiRequest shouldBe applicationApiRequest
    }
  }

}
