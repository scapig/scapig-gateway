package connectors

import java.util.UUID

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

class ApplicationConnectorSpec extends UnitSpec with BeforeAndAfterAll with BeforeAndAfterEach {
  val port = 7001

  val serverToken = "serverToken"
  val clientId = "clientId"
  val application = Application(Some(RateLimitTier.SILVER), UUID.randomUUID())

  val playApplication = new GuiceApplicationBuilder()
    .configure("services.application.port" -> "7001")
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
    val applicationConnector = playApplication.injector.instanceOf[ApplicationConnector]
  }

  "fetchByServerToken" should {
    "return the application" in new Setup {

      stubFor(get(s"/application?serverToken=$serverToken").willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody(Json.toJson(application).toString())))

      val result = await(applicationConnector.fetchByServerToken(serverToken))

      result shouldBe application
    }

    "fail with ApplicationNotFoundException when the serverToken does not match any application" in new Setup {

      stubFor(get(s"/application?serverToken=$serverToken").willReturn(aResponse()
        .withStatus(Status.NOT_FOUND)))

      intercept[ApplicationNotFoundException]{await(applicationConnector.fetchByServerToken(serverToken))}
    }

    "throw an exception when error" in new Setup {

      stubFor(get(s"/application?serverToken=$serverToken").willReturn(aResponse()
        .withStatus(Status.INTERNAL_SERVER_ERROR)))

      intercept[RuntimeException]{await(applicationConnector.fetchByServerToken(serverToken))}
    }
  }

  "fetchByClientId" should {
    "return the application" in new Setup {

      stubFor(get(s"/application?clientId=$clientId").willReturn(aResponse()
        .withStatus(Status.OK)
        .withBody(Json.toJson(application).toString())))

      val result = await(applicationConnector.fetchByClientId(clientId))

      result shouldBe application
    }

    "fail with ApplicationNotFoundException when the serverToken does not match any application" in new Setup {

      stubFor(get(s"/application?clientId=$clientId").willReturn(aResponse()
        .withStatus(Status.NOT_FOUND)))

      intercept[ApplicationNotFoundException]{await(applicationConnector.fetchByClientId(clientId))}
    }

    "throw an exception when error" in new Setup {

      stubFor(get(s"/application?clientId=$clientId").willReturn(aResponse()
        .withStatus(Status.INTERNAL_SERVER_ERROR)))

      intercept[RuntimeException]{await(applicationConnector.fetchByClientId(clientId))}
    }
  }

}
