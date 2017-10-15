package services

import connectors.DelegatedAuthorityConnector
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

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class DelegatedAuthorityServiceSpec extends UnitSpec with MockitoSugar {

  private val request = FakeRequest(
    method = "GET",
    uri = "/hello/world",
    headers = Headers(),
    body = AnyContentAsJson(Json.parse("""{}""")))

  private val accessToken = "accessToken"
  private val requestWithToken = request.withHeaders(HeaderNames.AUTHORIZATION -> s"Bearer $accessToken")

  private val apiRequest = ApiRequest(
    apiIdentifier = ApiIdentifier("context", "v1.1"),
    authType = AuthType.USER,
    apiEndpoint = "http://host.example/hello/world")

  private val delegatedAuthorityConnector = mock[DelegatedAuthorityConnector]
  private val authorityService = new DelegatedAuthorityService(delegatedAuthorityConnector)

  "fetchDelegatedAuthority" should {
    "throw DelegatedAuthorityNotFoundException exception when token is invalid" in {
      when(delegatedAuthorityConnector.fetchByAccessToken(accessToken)).thenReturn(failed(DelegatedAuthorityNotFoundException()))

      intercept[DelegatedAuthorityNotFoundException] {
        await(authorityService.fetchDelegatedAuthority(requestWithToken, accessToken, apiRequest))
      }
    }

    "throw DelegatedAuthorityNotFoundException exception when token has expired" in {
      when(delegatedAuthorityConnector.fetchByAccessToken(accessToken)).thenReturn(successful(authorityWithExpiration(now.minusMinutes(5))))

      intercept[DelegatedAuthorityNotFoundException] {
        await(authorityService.fetchDelegatedAuthority(requestWithToken, accessToken, apiRequest))
      }
    }

    "return the delegated authority when token is valid" in {
      val inFiveMinutes = now().plusMinutes(5)
      val unexpiredAuthority = authorityWithExpiration(inFiveMinutes)

      when(delegatedAuthorityConnector.fetchByAccessToken(accessToken)).thenReturn(successful(authorityWithExpiration(inFiveMinutes)))

      await(authorityService.fetchDelegatedAuthority(requestWithToken, accessToken, apiRequest)) shouldBe unexpiredAuthority
    }
  }

  private def authorityWithExpiration(expirationDateTime: DateTime) =
    DelegatedAuthority("clientId", "userId", Environment.PRODUCTION, Token(expirationDateTime, Set.empty))

}
