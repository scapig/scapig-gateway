package services

import connectors.DelegatedAuthorityConnector
import models.GatewayError.{InvalidCredentials, MissingCredentials}
import models._
import org.joda.time.DateTime
import org.joda.time.DateTime.now
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Headers}
import play.api.test.FakeRequest
import utils.UnitSpec

import scala.concurrent.Future.successful

class DelegatedAuthorityServiceSpec extends UnitSpec with MockitoSugar {

  private val request = FakeRequest(
    method = "GET",
    uri = "/hello/world",
    headers = Headers(),
    body = AnyContentAsJson(Json.parse("""{}""")))

  private val accessToken = "accessToken"
  private val requestWithToken = request.withHeaders(HeaderNames.AUTHORIZATION -> "Bearer 31c99f9482de49544c6cc3374c378028")

  private val apiRequest = ApiRequest(
    apiIdentifier = ApiIdentifier("context", "v1.1"),
    authType = AuthType.USER,
    apiEndpoint = "http://host.example/hello/world")

  private val delegatedAuthorityConnector = mock[DelegatedAuthorityConnector]
  private val authorityService = new DelegatedAuthorityService(delegatedAuthorityConnector)

  "findAuthority" should {

    "throw DelegatedAuthorityNotFoundException exception when credentials have expired" in {
      mockDelegatedAuthorityConnector(accessToken, authorityWithExpiration(now.minusMinutes(5)))

      intercept[DelegatedAuthorityNotFoundException] {
        await(authorityService.findAuthority(requestWithToken, accessToken, apiRequest))
      }
    }

    "return the delegated authority when credentials are valid" in {
      val inFiveMinutes = now().plusMinutes(5)
      val unexpiredAuthority = authorityWithExpiration(inFiveMinutes)

      mockDelegatedAuthorityConnector(accessToken, authorityWithExpiration(inFiveMinutes))

      await(authorityService.findAuthority(requestWithToken, accessToken, apiRequest)) shouldBe unexpiredAuthority
    }
  }

  private def mockDelegatedAuthorityConnector(accessToken: String, authority: DelegatedAuthority) =
    when(delegatedAuthorityConnector.fetchByAccessToken(accessToken)).thenReturn(successful(authority))

  private def authorityWithExpiration(expirationDateTime: DateTime) =
    DelegatedAuthority("clientId", "userOID", Environment.PRODUCTION, Token(expirationDateTime, Set.empty))

}
