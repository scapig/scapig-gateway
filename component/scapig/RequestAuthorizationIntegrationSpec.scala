package scapig

import java.util.UUID

import models.RateLimitTier.BRONZE
import models._
import org.joda.time.DateTime.now
import play.api.http.Status._
import play.mvc.Http.HeaderNames.{ACCEPT, AUTHORIZATION}
import scapig.stubs.ApiStub

import scalaj.http.Http

class RequestAuthorizationIntegrationSpec extends BaseFeatureSpec {

  private val anApiDefinition = ApiDefinition("api-simulator", s"http://localhost:${ApiStub.port}",
    Seq(
      APIVersion("1.0", APIStatus.PUBLISHED, Seq(
        Endpoint("userScope1", HttpMethod.GET, AuthType.USER, scope = Some("scope1")),
        Endpoint("userScope2", HttpMethod.GET, AuthType.USER, scope = Some("scope2")),
        Endpoint("application", HttpMethod.GET, AuthType.APPLICATION),
        Endpoint("open", HttpMethod.GET, AuthType.NONE))
      ))
    )
  private val apiResponse = """{"response": "ok"}"""
  private val accessToken = "accessToken"
  private val clientId = "clientId"

  private val authority = DelegatedAuthority(clientId, "userId", Environment.PRODUCTION, Token(now().plusHours(3), Set("scope1")))

  private val applicationId = UUID.randomUUID()
  private val application = EnvironmentApplication(applicationId, "clientId", BRONZE)
  private val apiIdentifier = ApiIdentifier("api-simulator", anApiDefinition.versions.head.version)

  override def beforeEach() {
    super.beforeEach()

    Given("An API Definition exists")
    apiDefinitionStub.willReturnTheApiDefinition(apiIdentifier.context, anApiDefinition)

    And("The API returns a response")
    apiStub.willReturnTheResponse(apiResponse)
  }

  feature("User Restricted endpoint") {

    scenario("A user restricted request without an 'authorization' http header is not proxied") {

      Given("A request without an 'authorization' http header")
      val httpRequest = Http(s"$serviceUrl/api-simulator/userScope1").header(ACCEPT, "application/vnd.mybusiness.1.0+json")

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '401' unauthorized")
      assertCodeIs(httpResponse, UNAUTHORIZED)

      And("The response message code is 'MISSING_CREDENTIALS'")
      assertBodyIs(httpResponse, """ {"code":"MISSING_CREDENTIALS","message":"Authentication information is not provided"} """)
    }

    scenario("A user restricted request with an invalid 'authorization' http header is not proxied") {

      Given("A request with an invalid access token")
      val httpRequest = Http(s"$serviceUrl/api-simulator/userScope1")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("An authority does not exist for the access token")
      delegatedAuthorityStub.willNotReturnAnAuthorityForAccessToken(accessToken)

      And("The token does not match any application")
      applicationStub.willNotFindAnApplicationForServerToken(serverToken = accessToken)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '401' unauthorized")
      assertCodeIs(httpResponse, UNAUTHORIZED)

      And("The response message code is 'INVALID_CREDENTIALS'")
      assertBodyIs(httpResponse, """ {"code":"INVALID_CREDENTIALS","message":"Invalid Authentication information provided"} """)
    }

    scenario("A user restricted request attempting to use a valid server token is not proxied") {

      val serverToken = "serverToken"

      Given("A request with a server token")
      val httpRequest = Http(s"$serviceUrl/api-simulator/userScope1")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $serverToken")

      And("An authority does not exist for the token")
      delegatedAuthorityStub.willNotReturnAnAuthorityForAccessToken(serverToken)

      And("An application exists for the server token")
      applicationStub.willReturnTheApplicationForServerToken(serverToken, application)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '401' unauthorized")
      assertCodeIs(httpResponse, UNAUTHORIZED)

      And("The response message code is 'INVALID_CREDENTIALS'")
      assertBodyIs(httpResponse, """ {"code":"INVALID_CREDENTIALS","message":"Invalid Authentication information provided"} """)
    }

    scenario("A user restricted request, that fails with a NOT_FOUND when fetching the application by authority, is not proxied") {
      Given("A request to an endpoint requiring 'scope1'")
      val httpRequest = Http(s"$serviceUrl/api-simulator/userScope1")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("The access token matches an authority with 'scope1'")
      delegatedAuthorityStub.willReturnTheAuthorityForAccessToken(accessToken, authority)

      And("An application is not found for the delegated authority")
      applicationStub.willNotFindAnApplicationForClientId(clientId)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '500' internal server error")
      assertCodeIs(httpResponse, INTERNAL_SERVER_ERROR)

      And("The response message code is 'SERVER_ERROR'")
      assertBodyIs(httpResponse, """ {"code":"SERVER_ERROR","message":"Internal server error"} """)
    }

    scenario("A user restricted request, that fails when fetching the application by authority, is not proxied") {
      Given("A request to an endpoint requiring 'scope1'")
      val httpRequest = Http(s"$serviceUrl/api-simulator/userScope1")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("The access token matches an authority with 'scope1'")
      delegatedAuthorityStub.willReturnTheAuthorityForAccessToken(accessToken, authority)

      And("There is an error while retrieving the application by client id")
      applicationStub.willFailFindingTheApplicationForClientId(clientId)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '500' internal server error")
      assertCodeIs(httpResponse, INTERNAL_SERVER_ERROR)

      And("The response message code is 'SERVER_ERROR'")
      assertBodyIs(httpResponse, """ {"code":"SERVER_ERROR","message":"Internal server error"} """)
    }

    scenario("A user restricted request that fails when fetching the application subscriptions is not proxied") {

      Given("A request to an endpoint requiring 'scope1'")
      val httpRequest = Http(s"$serviceUrl/api-simulator/userScope1")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("The access token matches an authority with 'scope1'")
      delegatedAuthorityStub.willReturnTheAuthorityForAccessToken(accessToken, authority)

      And("An application exists for the delegated authority")
      applicationStub.willReturnTheApplicationForClientId(clientId, application)

      And("There is a failure while finding the application subscriptions")
      applicationStub.willFailWhenFetchingTheSubscription(applicationId.toString, apiIdentifier)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '500' internal server error")
      assertCodeIs(httpResponse, INTERNAL_SERVER_ERROR)

      And("The response message code is 'SERVER_ERROR'")
      assertBodyIs(httpResponse, """ {"code":"SERVER_ERROR","message":"Internal server error"} """)
    }

    scenario("A user restricted request with an application not subscribed to the API is not proxied") {

      Given("A request to an endpoint requiring 'scope1'")
      val httpRequest = Http(s"$serviceUrl/api-simulator/userScope1")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("The access token matches an authority with 'scope1'")
      delegatedAuthorityStub.willReturnTheAuthorityForAccessToken(accessToken, authority)

      And("An application exists for the delegated authority")
      applicationStub.willReturnTheApplicationForClientId(clientId, application)

      And("The application is not subscribed")
      applicationStub.willNotFindASubscriptionFor(applicationId.toString, apiIdentifier)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '403' forbidden")
      assertCodeIs(httpResponse, FORBIDDEN)

      And("The response message code is 'RESOURCE_FORBIDDEN'")
      assertBodyIs(httpResponse, """ {"code":"RESOURCE_FORBIDDEN","message":"The application is not subscribed to the API which it is attempting to invoke"} """)
    }

    scenario("A user restricted request with invalid scopes is not proxied") {
      Given("A request to an endpoint requiring 'scope2'")
      val httpRequest = Http(s"$serviceUrl/api-simulator/userScope2")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("The access token matches an authority with 'scope1'")
      delegatedAuthorityStub.willReturnTheAuthorityForAccessToken(accessToken, authority)

      And("An application exists for the delegated authority")
      applicationStub.willReturnTheApplicationForClientId(clientId, application)

      And("The application is subscribed to the correct API")
      applicationStub.willFindTheSubscriptionFor(applicationId.toString, apiIdentifier)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '403' forbidden")
      assertCodeIs(httpResponse, FORBIDDEN)

      And("The response message code is 'INVALID_SCOPE'")
      assertBodyIs(httpResponse, """ {"code":"INVALID_SCOPE","message":"Cannot access the required resource. Ensure this token has all the required scopes."} """)
    }

    scenario("A request passing checks for a user restricted endpoint is proxied") {

      Given("A request to an endpoint requiring 'scope1'")
      val httpRequest = Http(s"$serviceUrl/api-simulator/userScope1")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("The access token matches an authority with 'scope1'")
      delegatedAuthorityStub.willReturnTheAuthorityForAccessToken(accessToken, authority)

      And("An application exists for the delegated authority")
      applicationStub.willReturnTheApplicationForClientId(clientId, application)

      And("The application is subscribed to the correct API")
      applicationStub.willFindTheSubscriptionFor(applicationId.toString, apiIdentifier)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The request is proxied to the microservice")
      assertCodeIs(httpResponse, OK)
      assertBodyIs(httpResponse, apiResponse)
    }
  }


  feature("Application Restricted endpoint") {

    scenario("An application restricted request without an 'authorization' http header is not proxied") {

      Given("A request without an 'authorization' http header")
      val httpRequest = Http(s"$serviceUrl/api-simulator/application").header(ACCEPT, "application/vnd.mybusiness.1.0+json")

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '401' unauthorized")
      assertCodeIs(httpResponse, UNAUTHORIZED)

      And("The response message code is 'MISSING_CREDENTIALS'")
      assertBodyIs(httpResponse, """ {"code":"MISSING_CREDENTIALS","message":"Authentication information is not provided"} """)
    }

    scenario("An application restricted request with a valid server token is proxied") {

      Given("A request with valid headers")
      val httpRequest = Http(s"$serviceUrl/api-simulator/application")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("The server token matches an application")
      applicationStub.willReturnTheApplicationForServerToken(serverToken = accessToken, application)

      And("The application is subscribed to the correct API")
      applicationStub.willFindTheSubscriptionFor(applicationId.toString, apiIdentifier)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The request is proxied to the microservice")
      assertCodeIs(httpResponse, OK)
      assertBodyIs(httpResponse, apiResponse)
    }

    scenario("An application restricted request that matches a delegated authority is proxied") {

      Given("A request with valid headers")
      val httpRequest = Http(s"$serviceUrl/api-simulator/application")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("The access token does not match applications")
      applicationStub.willNotFindAnApplicationForServerToken(serverToken = accessToken)

      And("The access token matches the authority'")
      delegatedAuthorityStub.willReturnTheAuthorityForAccessToken(accessToken, authority)

      And("An application exists for the delegated authority")
      applicationStub.willReturnTheApplicationForClientId(clientId, application)

      And("The application is subscribed to the correct API")
      applicationStub.willFindTheSubscriptionFor(applicationId.toString, apiIdentifier)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The request is proxied to the microservice")
      assertCodeIs(httpResponse, OK)
      assertBodyIs(httpResponse, apiResponse)
    }

    scenario("An application restricted request, that fails with a NOT_FOUND when fetching the application " +
      "by server token and when retrieving the authority, is not proxied") {

      Given("A request with valid headers")
      val httpRequest = Http(s"$serviceUrl/api-simulator/application")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("No applications are found by server token")
      applicationStub.willNotFindAnApplicationForServerToken(serverToken = accessToken)

      And("The token does not match any authority")
      delegatedAuthorityStub.willNotReturnAnAuthorityForAccessToken(accessToken)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '401' unauthorized")
      assertCodeIs(httpResponse, UNAUTHORIZED)

      And("The response message code is 'UNAUTHORIZED'")
      assertBodyIs(httpResponse, """ {"code":"INVALID_CREDENTIALS","message":"Invalid Authentication information provided"} """)
    }

    scenario("An application restricted request, that fails with a NOT_FOUND when fetching the application " +
      "by server token and also by client id, is not proxied") {

      Given("A request with valid headers")
      val httpRequest = Http(s"$serviceUrl/api-simulator/application")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("No applications are found by server token")
      applicationStub.willNotFindAnApplicationForServerToken(serverToken = accessToken)

      And("The token matches the authority")
      delegatedAuthorityStub.willReturnTheAuthorityForAccessToken(accessToken, authority)

      And("No applications are found from the delegated authority")
      applicationStub.willNotFindAnApplicationForClientId(clientId)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '500' internal server error")
      assertCodeIs(httpResponse, INTERNAL_SERVER_ERROR)

      And("The response message code is 'SERVER_ERROR'")
      assertBodyIs(httpResponse, """ {"code":"SERVER_ERROR","message":"Internal server error"} """)
    }

    scenario("An application restricted request failing finding subscriptions is not proxied") {

      Given("A request with valid headers")
      val httpRequest = Http(s"$serviceUrl/api-simulator/application")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("The server token matches an application")
      applicationStub.willReturnTheApplicationForServerToken(serverToken = accessToken, application)

      And("There is an error while fetching the application subscriptions")
      applicationStub.willFailWhenFetchingTheSubscription(applicationId.toString, apiIdentifier)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '500' internal server error")
      assertCodeIs(httpResponse, INTERNAL_SERVER_ERROR)

      And("The response message code is 'SERVER_ERROR'")
      assertBodyIs(httpResponse, """ {"code":"SERVER_ERROR","message":"Internal server error"} """)
    }

    scenario("An application restricted request with an application not subscribed to the API is not proxied") {

      Given("A request with valid headers")
      val httpRequest = Http(s"$serviceUrl/api-simulator/application")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")
        .header(AUTHORIZATION, s"Bearer $accessToken")

      And("The server token matches an application")
      applicationStub.willReturnTheApplicationForServerToken(serverToken = accessToken, application)

      And("The application is not subscribed")
      applicationStub.willNotFindASubscriptionFor(applicationId.toString, apiIdentifier)

      When("The request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The http response is '403' forbidden")
      assertCodeIs(httpResponse, FORBIDDEN)

      And("The response message code is 'RESOURCE_FORBIDDEN'")
      assertBodyIs(httpResponse, """ {"code":"RESOURCE_FORBIDDEN","message":"The application is not subscribed to the API which it is attempting to invoke"} """)
    }
  }


  feature("Open endpoint") {

    scenario("A request passing checks for an open endpoint is proxied") {

      Given("a request which passes checks for an open endpoint")
      val httpRequest = Http(s"$serviceUrl/api-simulator/open")
        .header(ACCEPT, "application/vnd.mybusiness.1.0+json")

      When("the request is sent to the gateway")
      val httpResponse = invoke(httpRequest)

      Then("The request is proxied to the microservice")
      assertCodeIs(httpResponse, OK)
      assertBodyIs(httpResponse, apiResponse)
    }
  }
}
