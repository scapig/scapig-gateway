package services.routing

import models.GatewayError._
import models._
import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsJson, Headers, Request}
import play.api.test.FakeRequest
import services.{ApplicationService, DelegatedAuthorityService, RoutingServicesMocks, ScopeValidator}
import utils.UnitSpec

import scala.concurrent.Future._

class UserRestrictedEndpointServiceSpec extends UnitSpec with MockitoSugar with RoutingServicesMocks {

  private val fakeRequest = FakeRequest(
    method = "GET",
    uri = "http://host.example/foo",
    headers = Headers(HeaderNames.AUTHORIZATION -> "Bearer accessToken"),
    body = AnyContentAsJson(Json.parse("""{}""")))

  private val apiRequest = ApiRequest(
    apiIdentifier = ApiIdentifier("context", "version"),
    authType = AuthType.USER,
    apiEndpoint = "http://host.example/foo/context",
    scope = Some("scopeMoo"))

  private trait Setup {
    val delegatedAuthorityService = mock[DelegatedAuthorityService]
    val applicationService = mock[ApplicationService]
    val scopeValidator = mock[ScopeValidator]

    val userRestrictedEndpointService = new UserRestrictedEndpointService(delegatedAuthorityService, applicationService, scopeValidator)

    val clientId = "clientId"
    val application = anApplication()
  }

  "routeRequest" should {

    "fail without a valid access token" in new Setup {
      val mockApiRequest = mock[ApiRequest]
      mockAuthority(delegatedAuthorityService, MissingCredentials(mock[Request[AnyContent]], mockApiRequest))

      intercept[MissingCredentials] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "decline a request not matching a delegated authority" in new Setup {
      val mockApiRequest = mock[ApiRequest]
      mockAuthority(delegatedAuthorityService, InvalidCredentials(mock[Request[AnyContent]], mockApiRequest))

      val caught = intercept[InvalidCredentials] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "decline a request with a valid server token" in new Setup {
      val serverToken = "serverToken"
      val request = fakeRequest.withHeaders(HeaderNames.AUTHORIZATION -> serverToken)

      mockAuthority(delegatedAuthorityService, DelegatedAuthorityNotFoundException())
      mockApplicationByServerToken(applicationService, serverToken, application)

      intercept[IncorrectAccessTokenType] {
        await(userRestrictedEndpointService.routeRequest(request, ProxyRequest(request), apiRequest))
      }
    }

    "propagate the error, when there is a failure in fetching the application" in new Setup {
      mockAuthority(delegatedAuthorityService, validAuthority())
      mockScopeValidation(scopeValidator)
      mockApplicationByClientId(applicationService, clientId, ServerError())

      intercept[ServerError] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "decline a request not matching the application API subscriptions" in new Setup {
      mockAuthority(delegatedAuthorityService, validAuthority())
      mockScopeValidation(scopeValidator)
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, failed(InvalidSubscription()))

      intercept[InvalidSubscription] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "decline a request not matching scopes" in new Setup {
      mockAuthority(delegatedAuthorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, successful(()))
      mockScopeValidation(scopeValidator, InvalidScope())

      intercept[InvalidScope] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "propagate the error, when the application has reached its rate limit" in new Setup {
      mockAuthority(delegatedAuthorityService, validAuthority())
      mockScopeValidation(scopeValidator)
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, failed(ThrottledOut()))

      intercept[ThrottledOut] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "route a request which meets all requirements" in new Setup {
      mockAuthority(delegatedAuthorityService, validAuthority())
      mockScopeValidation(scopeValidator)
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, successful(()))

      val expectedResult = apiRequest.copy(
        userOid = Some("userOID"),
        clientId = Some("clientId")
      )

      val result = await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))

      result shouldBe expectedResult
    }

  }

}
