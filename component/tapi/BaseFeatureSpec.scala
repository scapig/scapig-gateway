package tapi

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json._
import tapi.stubs.{ApiDefinitionStub, ApiStub, ApplicationStub, DelegatedAuthorityStub}

import scala.concurrent.duration._
import scalaj.http.{HttpRequest, HttpResponse}

abstract class BaseFeatureSpec extends FeatureSpec with GivenWhenThen with Matchers with BeforeAndAfterAll
with GuiceOneServerPerSuite with BeforeAndAfterEach {

  override lazy val port = 19111

  implicit override lazy val app: Application =  new GuiceApplicationBuilder().configure(
    "services.api-definition.port" -> ApiDefinitionStub.stub.port,
    "services.application.port" -> ApplicationStub.stub.port,
    "services.delegated-authority.port" -> DelegatedAuthorityStub.stub.port
  ).build()

  val serviceUrl = s"http://localhost:$port"
  val timeout = 10.second

  val apiDefinitionStub = ApiDefinitionStub
  val apiStub = ApiStub
  val delegatedAuthorityStub = DelegatedAuthorityStub
  val applicationStub = ApplicationStub
  val mocks = Seq(apiDefinitionStub, apiStub, delegatedAuthorityStub, applicationStub)

  override protected def beforeAll(): Unit = {
    mocks.foreach(m => if (!m.stub.server.isRunning) m.stub.server.start())
  }

  override protected def afterEach(): Unit = {
    mocks.foreach(_.stub.mock.resetMappings())
  }

  override protected def afterAll(): Unit = {
    mocks.foreach(_.stub.server.stop())
  }

  protected def invoke(httpRequest: HttpRequest): HttpResponse[String] = httpRequest.asString

  protected def assertCodeIs(httpResponse: HttpResponse[String], expectedHttpCode: Int) =
    httpResponse.code shouldBe expectedHttpCode

  protected def assertBodyIs(httpResponse: HttpResponse[String], expectedJsonBody: String) =
    parse(httpResponse.body) shouldBe parse(expectedJsonBody)
}

case class MockHost(port: Int) {
  val server = new WireMockServer(WireMockConfiguration.wireMockConfig().port(port))
  val mock = new WireMock("localhost", port)
}

trait Stub {
  val stub: MockHost
}
