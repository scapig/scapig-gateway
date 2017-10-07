package config

import javax.inject.Singleton

import models.{ErrorInternalServerError, ErrorInvalidRequest, ErrorNotFound}
import play.api.Logger
import play.api.http.DefaultHttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Result, Results}
import models.JsonFormatters._

import scala.concurrent.Future

@Singleton
class ErrorHandler extends DefaultHttpErrorHandler {
  override def onBadRequest(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(ErrorInvalidRequest(message).toHttpResponse)
  }

  override def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(ErrorNotFound().toHttpResponse)
  }

  override def onServerError(request: RequestHeader, exception: Throwable) = {
    Logger.error("An unexpected error occurred", exception)
    Future.successful(ErrorInternalServerError(exception.getMessage).toHttpResponse)
  }
}