package connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import models.JsonFormatters._
import models._
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.Status
import utils.UnitSpec

class DelegatedAuthorityConnectorSpec extends UnitSpec with BeforeAndAfterAll with BeforeAndAfterEach {
  val port = 7001

  val accessToken = "accessToken"
  val token = Token(DateTime.now().plusHours(4), Set("scope1"))
  val delegatedAuthority = DelegatedAuthority("clientId", "userId", Environment.PRODUCTION, token)

  val playApplication = new GuiceApplicationBuilder()
    .configure("services.delegated-authority.host" -> "localhost")
    .configure("services.delegated-authority.port" -> "7001")
    .build()
  val wireMockServer = new WireMockServer(wireMockConfig().port(port))

  override def beforeAll {
    configureFor(port)
    wireMockServer.start()
  }

  override def afterAll: Unit = {
    wireMockServer.stop()
  }

  override def beforeEach(): Unit = {
    WireMock.reset()
  }

  trait Setup {
    val delegatedAuthorityConnector = playApplication.injector.instanceOf[DelegatedAuthorityConnector]
  }

  "fetchByAccessToken" should {
    "return the authority" in new Setup {

      stubFor(get(s"/authority?accessToken=$accessToken").willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody(Json.toJson(delegatedAuthority).toString())))

      val result = await(delegatedAuthorityConnector.fetchByAccessToken(accessToken))

      result shouldBe delegatedAuthority
    }

    "fail with DelegatedAuthorityNotFoundException when the context does not match any API" in new Setup {

      stubFor(get(s"/authority?accessToken=$accessToken").willReturn(aResponse()
        .withStatus(Status.NOT_FOUND)))

      intercept[DelegatedAuthorityNotFoundException]{await(delegatedAuthorityConnector.fetchByAccessToken(accessToken))}
    }

    "throw an exception when error" in new Setup {

      stubFor(get(s"/authority?accessToken=$accessToken").willReturn(aResponse()
        .withStatus(Status.INTERNAL_SERVER_ERROR)))

      intercept[RuntimeException]{await(delegatedAuthorityConnector.fetchByAccessToken(accessToken))}
    }
  }
}
