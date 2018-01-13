package connectors

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import models.GatewayError.ApiNotFound
import models._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.mvc.Http.Status
import utils.UnitSpec
import models.JsonFormatters._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class ApiDefinitionConnectorSpec extends UnitSpec with BeforeAndAfterAll with BeforeAndAfterEach {
  val port = 7001

  val apiContext = "calendar"
  val apiEndpoint = Endpoint("/today", HttpMethod.GET, AuthType.NONE)
  val apiVersion = APIVersion("1.0", APIStatus.PROTOTYPED, "http://service-host", Seq(apiEndpoint))
  val apiDefinition = ApiDefinition("calendar-service", Seq(apiVersion))

  val playApplication = new GuiceApplicationBuilder()
    .configure("services.api-definition.host" -> "localhost")
    .configure("services.api-definition.port" -> "7001")
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
    val apiDefinitionConnector = playApplication.injector.instanceOf[ApiDefinitionConnector]
  }

  "fetchByContext" should {
    "return the API" in new Setup {

      stubFor(get(s"/api-definition?context=$apiContext").willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody(Json.toJson(apiDefinition).toString())))

      val result = await(apiDefinitionConnector.fetchByContext(apiContext))

      result shouldBe apiDefinition
    }

    "fail with ApiNotFound when the context does not match any API" in new Setup {

      stubFor(get(s"/api-definition?context=$apiContext").willReturn(aResponse()
        .withStatus(Status.NOT_FOUND)))

      intercept[ApiNotFound]{await(apiDefinitionConnector.fetchByContext(apiContext))}
    }

    "throw an exception when error" in new Setup {

      stubFor(get(s"/api-definition?context=$apiContext").willReturn(aResponse()
        .withStatus(Status.INTERNAL_SERVER_ERROR)))

      intercept[RuntimeException]{await(apiDefinitionConnector.fetchByContext(apiContext))}
    }
  }
}
