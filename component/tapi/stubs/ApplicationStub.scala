package tapi.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, get, urlPathEqualTo}
import models.{ApiIdentifier, EnvironmentApplication}
import play.api.http.Status._
import play.api.libs.json.Json.{stringify, toJson}
import tapi.{MockHost, Stub}
import models.JsonFormatters._

object ApplicationStub extends Stub {

  override val stub = MockHost(22223)

  def willReturnTheApplicationForServerToken(serverToken: String, application: EnvironmentApplication) =
    stub.mock.register( get(urlPathEqualTo("/application"))
      .withQueryParam("serverToken", equalTo(serverToken))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(stringify(toJson(application)))))

  def willNotFindAnApplicationForServerToken(serverToken: String) =
    stub.mock.register(get(urlPathEqualTo("/application"))
      .withQueryParam("serverToken", equalTo(serverToken))
      .willReturn(
        aResponse().withStatus(NOT_FOUND)
      ))

  def willFailFindingTheApplicationForServerToken(serverToken: String) =
    stub.mock.register(get(urlPathEqualTo("/application"))
      .withQueryParam("serverToken", equalTo(serverToken))
      .willReturn(
        aResponse().withStatus(BAD_GATEWAY)
      ))


  def willReturnTheApplicationForClientId(clientId: String, application: EnvironmentApplication) =
    stub.mock.register(get(urlPathEqualTo("/application"))
      .withQueryParam("clientId", equalTo(clientId))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(stringify(toJson(application)))
      ))

  def willNotFindAnApplicationForClientId(clientId: String) =
    stub.mock.register(get(urlPathEqualTo("/application"))
      .withQueryParam("clientId", equalTo(clientId))
      .willReturn(
        aResponse().withStatus(NOT_FOUND)
      ))

  def willFailFindingTheApplicationForClientId(clientId: String) =
    stub.mock.register(get(urlPathEqualTo("/application"))
      .withQueryParam("clientId", equalTo(clientId))
      .willReturn(
        aResponse().withStatus(GATEWAY_TIMEOUT)
      ))


  def willFindTheSubscriptionFor(applicationId: String, api: ApiIdentifier) =
    stub.mock.register(get(urlPathEqualTo(s"/application/$applicationId/subscription/${api.context}/${api.version}"))
      .willReturn(
        aResponse()
          .withStatus(NO_CONTENT)
      ))

  def willNotFindASubscriptionFor(applicationId: String, api: ApiIdentifier) =
    stub.mock.register(get(urlPathEqualTo(s"/application/$applicationId/subscription/${api.context}/${api.version}"))
      .willReturn(
        aResponse().withStatus(NOT_FOUND)
      ))

  def willFailWhenFetchingTheSubscription(applicationId: String, api: ApiIdentifier) =
    stub.mock.register(get(urlPathEqualTo(s"/application/$applicationId/subscription/${api.context}/${api.version}"))
      .willReturn(
        aResponse().withStatus(INTERNAL_SERVER_ERROR)
      ))
}
