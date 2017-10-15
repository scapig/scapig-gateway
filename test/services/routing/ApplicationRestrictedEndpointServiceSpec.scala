package services.routing

import models.GatewayError._
import models._
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Headers}
import play.api.test.FakeRequest
import play.mvc.Http.HeaderNames
import services.{ApplicationService, DelegatedAuthorityService, RoutingServicesMocks}
import utils.UnitSpec

import scala.concurrent.Future.{failed, successful}

class ApplicationRestrictedEndpointServiceSpec extends UnitSpec with MockitoSugar with RoutingServicesMocks {

  private trait Setup {

    val serverToken = "accessToken"
    val clientId = "clientId"
    val application = anApplication()

    val apiRequest = ApiRequest(
      apiIdentifier = ApiIdentifier("context", "version"),
      authType = AuthType.APPLICATION,
      apiEndpoint = "http://host.example/foo/context")

    val basicRequest = FakeRequest(
      method = "GET",
      uri = "http://host.example/foo",
      headers = Headers(),
      body = AnyContentAsJson(Json.parse("""{}""")))

    val applicationRequestWithToken = basicRequest.withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $serverToken")

    val delegatedAuthorityService = mock[DelegatedAuthorityService]
    val applicationService = mock[ApplicationService]

    val applicationRestrictedEndpointService = new ApplicationRestrictedEndpointService(delegatedAuthorityService, applicationService)
  }

  "routeRequest" should {
    "route a request with a valid access token that meets all requirements" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, ApplicationNotFoundException())
      mockAuthority(delegatedAuthorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, successful(()))

      val expectedResult = apiRequest.copy(clientId = Some(clientId))
      val result = await(applicationRestrictedEndpointService.routeRequest(applicationRequestWithToken, ProxyRequest(applicationRequestWithToken), apiRequest))

      result shouldBe expectedResult
    }

    "route a request with a valid server token that meets all requirements" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, successful(()))

      val expectedResult = apiRequest.copy(clientId = Some(clientId))
      val result = await(applicationRestrictedEndpointService.routeRequest(applicationRequestWithToken, ProxyRequest(applicationRequestWithToken), apiRequest))

      result shouldBe expectedResult
    }

    "fail with MissingCredentials when the accessToken is missing" in new Setup {
      intercept[MissingCredentials] {
        await(applicationRestrictedEndpointService.routeRequest(basicRequest, ProxyRequest(basicRequest), apiRequest))
      }
    }

    "fail, with InvalidCredentials when the accessToken does not match any application or delegated authority" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, ApplicationNotFoundException())
      mockAuthority(delegatedAuthorityService, DelegatedAuthorityNotFoundException())

      intercept[InvalidCredentials] {
        await(applicationRestrictedEndpointService.routeRequest(applicationRequestWithToken, ProxyRequest(applicationRequestWithToken), apiRequest))
      }
    }

    "fail with InvalidSubscription when the application is not subscribed" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, ApplicationNotFoundException())
      mockAuthority(delegatedAuthorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, failed(InvalidSubscription()))

      intercept[InvalidSubscription] {
        await(applicationRestrictedEndpointService.routeRequest(applicationRequestWithToken, ProxyRequest(applicationRequestWithToken), apiRequest))
      }
    }

    "fail with ThrottledOut when the application rate limit has been reached" in new Setup {
      mockApplicationByServerToken(applicationService, serverToken, ApplicationNotFoundException())
      mockAuthority(delegatedAuthorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, failed(ThrottledOut()))

      intercept[ThrottledOut] {
        await(applicationRestrictedEndpointService.routeRequest(applicationRequestWithToken, ProxyRequest(applicationRequestWithToken), apiRequest))
      }
    }
  }

}
