package tapi.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status.OK
import tapi.{MockHost, Stub}

object ApiStub extends Stub {

  val port = 22220
  val url = s"http://localhost:$port"

  override val stub = MockHost(port)

  def willReturnTheResponse(response: String) = {
    stub.mock.register(
      get(anyUrl())
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(response)))
  }
}
