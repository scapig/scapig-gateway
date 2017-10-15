package services.routing

import java.util.UUID

import connectors.ApiDefinitionConnector
import models.AuthType.NONE
import models.GatewayError.{MatchingResourceNotFound, NotFound}
import models.HttpMethod.GET
import models._
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeUtils._
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames.ACCEPT
import play.api.test.FakeRequest
import utils.UnitSpec

import scala.concurrent.Future
import scala.concurrent.Future._

class EndpointServiceSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  private val apiDefinitionConnector = mock[ApiDefinitionConnector]
  private val endpointService = new EndpointService(apiDefinitionConnector)
  private val apiDefinition = ApiDefinition(
    "api-context", "http://host.example", Seq(APIVersion("1.0", APIStatus.PUBLISHED, Seq(Endpoint("/api-endpoint", GET, NONE))))
  )
  private val fixedTimeInMillis = 11223344

  override def beforeEach(): Unit = {
    setCurrentMillisFixed(fixedTimeInMillis)
  }

  override def afterEach(): Unit = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  "Endpoint service" should {

    val request = FakeRequest("GET", "/api-context/api-endpoint").withHeaders(ACCEPT -> "application/vnd.mybusiness.1.0+json")
    val requestWithQueryString = FakeRequest("GET", "/api-context/api-endpoint?requiredParam=value").withHeaders(ACCEPT -> "application/vnd.mybusiness.1.0+json")

    "invoke api definition connector with correct service name" in {
      mockApiServiceConnectorToReturnSuccess

      await(endpointService.apiRequest(ProxyRequest(request), request))
      verify(apiDefinitionConnector).fetchByContext("api-context")
    }

    "return api definition when proxy request matches api definition endpoint" in {
      mockApiServiceConnectorToReturnSuccess

      val actualApiRequest = await(endpointService.apiRequest(ProxyRequest(request), request))

      apiRequest(actualApiRequest.requestId) shouldBe actualApiRequest
    }

    "fail with NotFound when no version matches the Accept headers in the API Definition" in {
      val notFoundRequest = request.withHeaders(ACCEPT -> "application/vnd.mybusiness.55.0+json")

      mockApiServiceConnectorToReturnSuccess

      intercept[NotFound]{
        await(endpointService.apiRequest(ProxyRequest(notFoundRequest), notFoundRequest))
      }
    }

    "fail with MatchingResourceNotFound when no endpoint matches in the API Definition" in {
      val invalidRequest = FakeRequest("GET", "/api-context/invalidEndpoint").withHeaders(ACCEPT -> "application/vnd.mybusiness.1.0+json")

      mockApiServiceConnectorToReturnSuccess

      intercept[MatchingResourceNotFound]{
        await(endpointService.apiRequest(ProxyRequest(invalidRequest), invalidRequest))
      }
    }

    "fail with MatchingResourceNotFound when a required request parameter is not in the URL" in {

      val anApiDefinition = ApiDefinition("api-context", "http://host.example", Seq(APIVersion("1.0", APIStatus.PUBLISHED,
        Seq(Endpoint("/api-endpoint", GET, NONE, queryParameters = Seq(Parameter("requiredParam", required = true)))))))

      mockApiServiceConnectorToReturn("api-context", successful(anApiDefinition))

      intercept[MatchingResourceNotFound]{
        await(endpointService.apiRequest(ProxyRequest(request), request))
      }
    }

    "succeed when all required request parameters are in the URL" in {

      val anApiDefinition = ApiDefinition("api-context", "http://host.example", Seq(APIVersion("1.0", APIStatus.PUBLISHED,
        Seq(Endpoint("/api-endpoint", GET, NONE, queryParameters = Seq(Parameter("requiredParam", required = true)))))))

      mockApiServiceConnectorToReturn("api-context", successful(anApiDefinition))

      val actualApiRequest = await(endpointService.apiRequest(ProxyRequest(requestWithQueryString), requestWithQueryString))

      apiRequest(actualApiRequest.requestId, urlWithQueryString) shouldBe actualApiRequest
    }

    "succeed when all request parameters in the URL are not required" in {

      val anApiDefinition = ApiDefinition("api-context", "http://host.example", Seq(APIVersion("1.0", APIStatus.PUBLISHED,
        Seq(Endpoint("/api-endpoint", GET, NONE, queryParameters = Seq(Parameter("requiredParam")))))))

      mockApiServiceConnectorToReturn("api-context", successful(anApiDefinition))

      val beforeTimeNanos = System.nanoTime()
      val actualApiRequest = await(endpointService.apiRequest(ProxyRequest(requestWithQueryString), requestWithQueryString))
      val afterTimeNanos = System.nanoTime()

      apiRequest(actualApiRequest.requestId, urlWithQueryString) shouldBe actualApiRequest
    }

    "succeed when at least one required request parameter is in the URL" in {

      val anApiDefinition = ApiDefinition("api-context", "http://host.example", Seq(APIVersion("1.0", APIStatus.PUBLISHED,
        Seq(Endpoint("/api-endpoint", GET, NONE, queryParameters = Seq(Parameter("requiredParam", required = true),
          Parameter("anotherRequiredParam", required = true)))))))

      mockApiServiceConnectorToReturn("api-context", successful(anApiDefinition))

      val beforeTimeNanos = System.nanoTime()
      val actualApiRequest = await(endpointService.apiRequest(ProxyRequest(requestWithQueryString), requestWithQueryString))
      val afterTimeNanos = System.nanoTime()

      apiRequest(actualApiRequest.requestId, urlWithQueryString) shouldBe actualApiRequest
    }

    "throw an exception when proxy request does not match api definition endpoint" in {

      mockApiServiceConnectorToReturnFailure

      intercept[RuntimeException] {
        await(endpointService.apiRequest(ProxyRequest(request), request))
      }
    }

  }

  private val basicUrl = "http://host.example//api-context/api-endpoint"
  private val urlWithQueryString = "http://host.example//api-context/api-endpoint?requiredParam=value"

  private def apiRequest(id: UUID = UUID.randomUUID(), url: String = basicUrl) = ApiRequest(
    apiIdentifier = ApiIdentifier("api-context", "1.0"),
    authType = NONE,
    apiEndpoint = url,
    requestId = id
  )

  private def mockApiServiceConnectorToReturnSuccess =
    mockApiServiceConnectorToReturn("api-context", successful(apiDefinition))

  private def mockApiServiceConnectorToReturnFailure =
    mockApiServiceConnectorToReturn("api-context", failed(new RuntimeException("simulated test exception")))

  private def mockApiServiceConnectorToReturn(context: String, eventualApiDefinition: Future[ApiDefinition]) =
    when(apiDefinitionConnector.fetchByContext(context)).thenReturn(eventualApiDefinition)

}
