package scapig.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, get, urlPathEqualTo}
import models.ApiDefinition
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json.{stringify, toJson}
import scapig.{MockHost, Stub}
import models.JsonFormatters._

object ApiDefinitionStub extends Stub {

  override val stub = MockHost(22221)

  def willReturnTheApiDefinition(context: String, apiDefinition: ApiDefinition) = {
    stub.mock.register(get(urlPathEqualTo("/api-definition")).withQueryParam("context", equalTo(context))
      .willReturn(aResponse().withStatus(OK)
        .withBody(stringify(toJson(apiDefinition)))))
  }

  def willNotReturnAnApiDefinitionForContext(context: String) = {
    stub.mock.register(get(urlPathEqualTo("/api-definition")).withQueryParam("context", equalTo(context))
      .willReturn(aResponse().withStatus(NOT_FOUND)))
  }
}
