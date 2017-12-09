package models

import java.net.URI

import models.GatewayError.MissingCredentials
import play.api.mvc.{AnyContent, Request, RequestHeader}
import play.mvc.Http.HeaderNames.AUTHORIZATION

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

case class ProxyRequest
(httpMethod: String,
 path: String,
 queryParameters: Map[String, Seq[String]] = Map.empty,
 headers: Map[String, String] = Map.empty,
 httpVersion: String = "HTTP/1.1") {

  def getHeader(name: String): Option[String] = headers.get(name)

  def accessToken(request: Request[AnyContent], apiRequest: ApiRequest): Future[String] = {
    getHeader(AUTHORIZATION) map (_.stripPrefix("Bearer ")) match {
      case Some(bearerToken) if bearerToken.trim.nonEmpty => successful(bearerToken)
      case _ => failed(MissingCredentials(request, apiRequest))
    }
  }

  lazy val rawPath = new URI(path).getRawPath
}

object ProxyRequest {

  def apply(requestHeader: RequestHeader): ProxyRequest = {
    ProxyRequest(
      requestHeader.method,
      requestHeader.uri.stripPrefix("/scapig-gateway"),
      requestHeader.queryString,
      requestHeader.headers.headers.toMap,
      requestHeader.version)
  }

}
