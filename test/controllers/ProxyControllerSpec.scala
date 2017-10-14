package controllers

import java.net.ConnectException
import java.util.UUID
import java.util.concurrent.TimeoutException

import akka.stream.Materializer
import com.google.common.net.{HttpHeaders => http}
import models.GatewayError.{InvalidCredentials, ServerError}
import models.Headers.X_REQUEST_ID
import models.{ApiRequest, GatewayError}
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.FakeRequest
import services.{ProxyService, RoutingService}
import utils.{RequestUtils, UnitSpec}
import models.JsonFormatters._

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class ProxyControllerSpec extends UnitSpec with MockitoSugar with RequestUtils {

  private implicit val materializer = mock[Materializer]

  private trait Setup {
    val request = FakeRequest("POST", "/hello/world")
    val apiRequest = mock[ApiRequest]
    val requestId = UUID.randomUUID().toString

    val proxyService = mock[ProxyService]
    def mockProxyService(result: Future[Result]) = {
      when(proxyService.proxy(request, apiRequest)(requestId)).thenReturn(result)
    }

    val routingService = mock[RoutingService]
    when(routingService.routeRequest(request)).thenReturn(successful(apiRequest))

    val proxyController = new ProxyController(proxyService, routingService)
  }

  "proxy" should {

    "propagate a downstream successful response" in new Setup {
      mockProxyService(successful(Ok(toJson("""{"foo":"bar"}"""))))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe toJson("""{"foo":"bar"}""")
      validateHeaders(result.header.headers, (X_REQUEST_ID, None))
    }

    "propagate a downstream error response" in new Setup {
      mockProxyService(successful(NotFound(toJson("Item Not Found"))))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe toJson("Item Not Found")
      validateHeaders(result.header.headers, (X_REQUEST_ID, None))
    }

    "convert exceptions to `InternalServerError` " in new Setup {
      mockProxyService(failed(new RuntimeException))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe toJson(ServerError())
    }

    "convert [502|503|504] responses" in new Setup {
      for (s <- List(BadGateway, ServiceUnavailable, GatewayTimeout)) {
        mockProxyService(successful(s))

        val result = await(proxyController.proxy()(requestId)(request))

        status(result) shouldBe SERVICE_UNAVAILABLE
        jsonBodyOf(result) shouldBe toJson(GatewayError.ServiceNotAvailable())
      }
    }

    "convert 501 responses" in new Setup {
      mockProxyService(successful(NotImplemented))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe NOT_IMPLEMENTED
      jsonBodyOf(result) shouldBe toJson(GatewayError.NotImplemented())
    }

    "convert request timeout errors" in new Setup {
      mockProxyService(failed(new TimeoutException()))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe SERVICE_UNAVAILABLE
      jsonBodyOf(result) shouldBe toJson(GatewayError.ServiceNotAvailable())
    }

    "convert connect timeout errors" in new Setup {
      mockProxyService(failed(new ConnectException()))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe SERVICE_UNAVAILABLE
      jsonBodyOf(result) shouldBe toJson(GatewayError.ServiceNotAvailable())
    }

    "return 401 when InvalidCredentials is thrown" in new Setup {
      when(routingService.routeRequest(any())).thenReturn(failed(InvalidCredentials(request, apiRequest)))

      val result = await(proxyController.proxy()(requestId)(request))

      status(result) shouldBe UNAUTHORIZED

      jsonBodyOf(result) shouldBe Json.obj("code" -> "INVALID_CREDENTIALS", "message" -> "Invalid Authentication information provided")
    }

  }

}
