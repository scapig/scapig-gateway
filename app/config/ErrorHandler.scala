package config

import java.net.ConnectException
import java.util.concurrent.TimeoutException
import javax.inject.Singleton

import models.GatewayError
import models.GatewayError.{ApiNotFound, ErrorInvalidRequest}
import models.JsonFormatters._
import play.api.Logger
import play.api.http.DefaultHttpErrorHandler
import play.api.libs.json.Json.toJson
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.Future
import scala.concurrent.Future.successful

@Singleton
class ErrorHandler extends DefaultHttpErrorHandler {
  override def onBadRequest(request: RequestHeader, message: String): Future[Result] = {
    successful(Results.Unauthorized(toJson(ErrorInvalidRequest(message))))
  }

  override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    successful(Results.Unauthorized(toJson(ApiNotFound())))
  }

  override def onServerError(request: RequestHeader, exception: Throwable) = { exception match {
    case e: GatewayError.MissingCredentials => successful(Results.Unauthorized(toJson(e)))
    case e: GatewayError.InvalidCredentials => successful(Unauthorized(toJson(e)))

    case e: GatewayError.InvalidScope => successful(Forbidden(toJson(e)))
    case e: GatewayError.InvalidSubscription => successful(Forbidden(toJson(e)))

    case e: GatewayError.MatchingResourceNotFound => successful(NotFound(toJson(e)))
    case e: GatewayError.ApiNotFound => successful(NotFound(toJson(e)))

    case e: GatewayError.ThrottledOut => successful(TooManyRequests(toJson(e)))

    case e: GatewayError.ServiceNotAvailable => successful(ServiceUnavailable(toJson(e)))

    case e: TimeoutException =>
      Logger.warn(s"Request timeout error for $request", e)
      successful(ServiceUnavailable(toJson(GatewayError.ServiceNotAvailable())))

    case e: ConnectException =>
      Logger.warn(s"Connect timeout error for $request", e)
      successful(ServiceUnavailable(toJson(GatewayError.ServiceNotAvailable())))

    case e =>
      Logger.error(s"Unexpected error for $request", e)
      successful(InternalServerError(toJson(GatewayError.ServerError())))
    }
  }
}