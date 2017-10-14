package controllers

import java.net.ConnectException
import java.util.concurrent.TimeoutException
import javax.inject.{Inject, Singleton}

import models.GatewayError
import models.JsonFormatters._
import play.api.Logger
import play.api.http.Status.{BAD_GATEWAY, GATEWAY_TIMEOUT, NOT_IMPLEMENTED, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json.toJson
import play.api.mvc.Results.{NotFound => PlayNotFound, _}
import play.api.mvc._
import services.{ProxyService, RoutingService}

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ProxyController @Inject()(proxyService: ProxyService, routingService: RoutingService) {

  private def newResult: (Result, Result) => Result = { (originalResult, newResult) =>
    Logger.warn(s"Api Gateway is converting a ${originalResult.header.status} response to ${newResult.header.status}")
    newResult
  }

  private def transformError: Result => Result = {
    result => result.header.status match {
      case NOT_IMPLEMENTED => newResult(result, NotImplemented(toJson(GatewayError.NotImplemented())))
      case BAD_GATEWAY | SERVICE_UNAVAILABLE | GATEWAY_TIMEOUT => newResult(result, ServiceUnavailable(toJson(GatewayError.ServiceNotAvailable())))
      case _ => result
    }
  }

  private def recoverError(request: Request[AnyContent])(implicit requestId: String): PartialFunction[Throwable, Result] = {
    case e: GatewayError.MissingCredentials => Unauthorized(toJson(e))
    case e: GatewayError.InvalidCredentials => Unauthorized(toJson(e))
    case e: GatewayError.IncorrectAccessTokenType => Unauthorized(toJson(e))

    case e: GatewayError.InvalidScope => Forbidden(toJson(e))
    case e: GatewayError.InvalidSubscription => Forbidden(toJson(e))

    case e: GatewayError.MatchingResourceNotFound => PlayNotFound(toJson(e))
    case e: GatewayError.NotFound => PlayNotFound(toJson(e))

    case e: GatewayError.ThrottledOut => TooManyRequests(toJson(e))

    case e: GatewayError.ServiceNotAvailable => ServiceUnavailable(toJson(e))

    case e: TimeoutException =>
      Logger.warn(s"Request timeout error for $request", e)
      ServiceUnavailable(toJson(GatewayError.ServiceNotAvailable()))

    case e: ConnectException =>
      Logger.warn(s"Connect timeout error for $request", e)
      ServiceUnavailable(toJson(GatewayError.ServiceNotAvailable()))

    case e =>
      Logger.error(s"Unexpected error for $request", e)
      InternalServerError(toJson(GatewayError.ServerError()))
  }

  def proxy()(implicit requestId: String) = Action.async(BodyParsers.parse.anyContent) { implicit request =>
    routingService.routeRequest(request) flatMap { apiRequest =>
      proxyService.proxy(request, apiRequest)
    } recover recoverError(request) map transformError
  }

}
