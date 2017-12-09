package scapig.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, get, urlPathEqualTo}
import models.DelegatedAuthority
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json.{stringify, toJson}
import scapig.{MockHost, Stub}
import models.JsonFormatters._

object DelegatedAuthorityStub extends Stub {
  override val stub = MockHost(22222)

  def willReturnTheAuthorityForAccessToken(accessToken: String, authority: DelegatedAuthority) =
    stub.mock.register(get(urlPathEqualTo("/authority"))
      .withQueryParam("accessToken", equalTo(accessToken))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(stringify(toJson(authority)))
      ))

  def willNotReturnAnAuthorityForAccessToken(accessToken: String) =
    stub.mock.register(get(urlPathEqualTo("/authority"))
      .withQueryParam("accessToken", equalTo(accessToken))
      .willReturn(
        aResponse().withStatus(NOT_FOUND)
      ))
}
