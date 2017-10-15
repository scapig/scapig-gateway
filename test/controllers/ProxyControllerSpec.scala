package controllers

import java.util.UUID

import akka.stream.Materializer
import models.ApiRequest
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.json.Json._
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.{FakeRequest, Helpers}
import services.ProxyService
import services.routing.RoutingService
import utils.UnitSpec

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class ProxyControllerSpec extends UnitSpec with MockitoSugar {

  private implicit val materializer = mock[Materializer]

  private trait Setup {
    val request = FakeRequest("POST", "/hello/world")
    val apiRequest = mock[ApiRequest]
    val requestId = UUID.randomUUID().toString

    val proxyService = mock[ProxyService]
    def mockProxyService(result: Future[Result]) = {
      when(proxyService.proxy(request, apiRequest)).thenReturn(result)
    }

    val routingService = mock[RoutingService]
    when(routingService.routeRequest(request)).thenReturn(successful(apiRequest))

    val proxyController = new ProxyController(Helpers.stubControllerComponents(), proxyService, routingService)
  }

  "proxy" should {

    "propagate a downstream successful response" in new Setup {
      mockProxyService(successful(Ok(toJson("""{"foo":"bar"}"""))))

      val result = await(proxyController.proxy()(request))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe toJson("""{"foo":"bar"}""")
    }

    "propagate a downstream error response" in new Setup {
      mockProxyService(successful(NotFound(toJson("Item Not Found"))))

      val result = await(proxyController.proxy()(request))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe toJson("Item Not Found")
    }

    "propagate exceptions" in new Setup {
      mockProxyService(failed(new RuntimeException))

      intercept[RuntimeException]{await(proxyController.proxy()(request))}
    }
  }

}
