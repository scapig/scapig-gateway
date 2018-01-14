package services.routing

import models.Environment.SANDBOX
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
    serviceBaseUrl = "http://host.example",
    path = "/foo",
    authType = AuthType.USER,
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

    "fail with MissingCredentials when the accessToken is missing" in new Setup {
      val request = fakeRequest.withHeaders(HeaderNames.AUTHORIZATION -> "")

      intercept[MissingCredentials] {
        await(userRestrictedEndpointService.routeRequest(request, ProxyRequest(request), apiRequest))
      }
    }

    "fail with InvalidCredentials when the accessToken is invalid" in new Setup {
      val mockApiRequest = mock[ApiRequest]
      mockAuthority(delegatedAuthorityService, DelegatedAuthorityNotFoundException())

      val caught = intercept[InvalidCredentials] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "fail with InvalidSubscription when the application is not subscribed to the API" in new Setup {
      mockAuthority(delegatedAuthorityService, validAuthority())
      mockScopeValidation(scopeValidator)
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, failed(InvalidSubscription()))

      intercept[InvalidSubscription] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "fail with InvalidScope when the delegatedAuthority does not have the required scopes" in new Setup {
      mockAuthority(delegatedAuthorityService, validAuthority())
      mockApplicationByClientId(applicationService, clientId, application)
      mockValidateSubscriptionAndRateLimit(applicationService, application, successful(()))
      mockScopeValidation(scopeValidator, InvalidScope())

      intercept[InvalidScope] {
        await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))
      }
    }

    "fail with ThrottledOut when the application has reached his rate limit" in new Setup {
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
        userId = Some("userId"),
        clientId = Some("clientId"),
        environment = Some(Environment.PRODUCTION)
      )

      val result = await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))

      result shouldBe expectedResult
    }

    "route a sandbox request which meets all requirements" in new Setup {
      val sandboxApplication = application.copy(environment = SANDBOX)
      val sandboxAuthority = validAuthority().copy(environment = SANDBOX)

      mockAuthority(delegatedAuthorityService, sandboxAuthority)
      mockScopeValidation(scopeValidator)
      mockApplicationByClientId(applicationService, clientId, sandboxApplication)
      mockValidateSubscriptionAndRateLimit(applicationService, sandboxApplication, successful(()))

      val result = await(userRestrictedEndpointService.routeRequest(fakeRequest, ProxyRequest(fakeRequest), apiRequest))

      result.environment shouldBe Some(SANDBOX)
    }

  }

}
