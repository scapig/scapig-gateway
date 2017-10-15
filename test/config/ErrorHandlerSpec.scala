package config

import java.net.ConnectException
import java.util.concurrent.TimeoutException

import models.ApiRequest
import models.GatewayError._
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.Json
import play.api.test.FakeRequest
import utils.UnitSpec

class ErrorHandlerSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val request = FakeRequest()
    val apiRequest = mock[ApiRequest]

    val handler = new ErrorHandler()
  }

  "onServerError" should {
    "return 401 when MissingCredentials is thrown" in new Setup {
      val result = await(handler.onServerError(request, MissingCredentials(request, apiRequest)))

      status(result) shouldBe Status.UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.obj("code" -> "MISSING_CREDENTIALS", "message" -> "Authentication information is not provided")
    }

    "return 401 when InvalidCredentials is thrown" in new Setup {
      val result = await(handler.onServerError(request, InvalidCredentials(request, apiRequest)))

      status(result) shouldBe Status.UNAUTHORIZED
      jsonBodyOf(result) shouldBe Json.obj("code" -> "INVALID_CREDENTIALS", "message" -> "Invalid Authentication information provided")
    }

    "return 403 when InvalidScope is thrown" in new Setup {
      val result = await(handler.onServerError(request, InvalidScope()))

      status(result) shouldBe Status.FORBIDDEN
      jsonBodyOf(result) shouldBe Json.obj("code" -> "INVALID_SCOPE", "message" -> "Cannot access the required resource. Ensure this token has all the required scopes.")
    }

    "return 403 when InvalidSubscription is thrown" in new Setup {
      val result = await(handler.onServerError(request, InvalidSubscription()))

      status(result) shouldBe Status.FORBIDDEN
      jsonBodyOf(result) shouldBe Json.obj("code" -> "RESOURCE_FORBIDDEN", "message" -> "The application is not subscribed to the API which it is attempting to invoke")
    }

    "return 404 when MatchingResourceNotFound is thrown" in new Setup {
      val result = await(handler.onServerError(request, MatchingResourceNotFound()))

      status(result) shouldBe Status.NOT_FOUND
      jsonBodyOf(result) shouldBe Json.obj("code" -> "MATCHING_RESOURCE_NOT_FOUND", "message" -> "A resource with the name in the request cannot be found in the API")
    }

    "return 404 when ApiNotFound is thrown" in new Setup {
      val result = await(handler.onServerError(request, ApiNotFound()))

      status(result) shouldBe Status.NOT_FOUND
      jsonBodyOf(result) shouldBe Json.obj("code" -> "NOT_FOUND", "message" -> "The requested resource could not be found.")
    }

    "return 429 when ThrottledOut is thrown" in new Setup {
      val result = await(handler.onServerError(request, ThrottledOut()))

      status(result) shouldBe Status.TOO_MANY_REQUESTS
      jsonBodyOf(result) shouldBe Json.obj("code" -> "MESSAGE_THROTTLED_OUT", "message" -> "The request for the API is throttled as you have exceeded your quota.")
    }

    "return 503 when ServiceNotAvailable is thrown" in new Setup {
      val result = await(handler.onServerError(request, ServiceNotAvailable()))

      status(result) shouldBe Status.SERVICE_UNAVAILABLE
      jsonBodyOf(result) shouldBe Json.obj("code" -> "SERVER_ERROR", "message" -> "Service unavailable")
    }

    "return 503 when TimeoutException is thrown" in new Setup {
      val result = await(handler.onServerError(request, new TimeoutException()))

      status(result) shouldBe Status.SERVICE_UNAVAILABLE
      jsonBodyOf(result) shouldBe Json.obj("code" -> "SERVER_ERROR", "message" -> "Service unavailable")
    }

    "return 503 when ConnectException is thrown" in new Setup {
      val result = await(handler.onServerError(request, new ConnectException()))

      status(result) shouldBe Status.SERVICE_UNAVAILABLE
      jsonBodyOf(result) shouldBe Json.obj("code" -> "SERVER_ERROR", "message" -> "Service unavailable")
    }

    "return 500 when a runtimeexception is thrown" in new Setup {
      val result = await(handler.onServerError(request, new RuntimeException()))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      jsonBodyOf(result) shouldBe Json.obj("code" -> "SERVER_ERROR", "message" -> "Internal server error")
    }

  }
}
