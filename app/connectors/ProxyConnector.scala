package connectors

import javax.inject.Inject

import akka.stream.scaladsl.Source
import config.AppContext
import models.Headers.{HOST, X_CLIENT_ID, X_REQUEST_ID}
import models.ApiRequest
import play.Logger
import play.api.http.{HttpChunk, HttpEntity}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class ProxyConnector @Inject()(appContext: AppContext, wsClient: WSClient) {

  def proxy(request: Request[AnyContent], apiRequest: ApiRequest): Future[Result] = {
    val headers = replaceHeaders(request.headers)(
      (HOST, None),
      (X_CLIENT_ID, apiRequest.clientId),
      (X_REQUEST_ID, Some(apiRequest.requestId.toString)))

    wsClient.url(apiRequest.apiEndpoint)
      .withMethod(request.method)
      .withHttpHeaders(headers.toSimpleMap.toSeq: _*)
      .withBody(bodyOf(request).getOrElse(""))
      .withRequestTimeout(appContext.requestTimeoutInMilliseconds.milliseconds)
      .execute.map { wsResponse =>
        val result = buildResult(wsResponse)
        Logger.info(s"request [$request] response [$wsResponse] result [$result]")
        result
      }
  }

  private def buildResult(streamedResponse: WSResponse): Result = {

    def flattenHeaders(headers: Map[String, Seq[String]]) = headers.mapValues(_.mkString(","))

    val body = Source.fromIterator(() =>
      Seq[HttpChunk](
        HttpChunk.Chunk(streamedResponse.bodyAsBytes),
        HttpChunk.LastChunk(Headers())
      ).iterator
    )

    Result(
      ResponseHeader(streamedResponse.status, flattenHeaders(streamedResponse.headers)),
      HttpEntity.Chunked(body, None)
    )
  }

  private def replaceHeaders(headers: Headers)(updatedHeaders: (String, Option[String])*): Headers = {
    updatedHeaders.headOption match {
      case Some((headerName, Some(headerValue))) => replaceHeaders(headers.replace(headerName -> headerValue))(updatedHeaders.tail:_*)
      case Some((headerName, None)) => replaceHeaders(headers.remove(headerName))(updatedHeaders.tail:_*)
      case None => headers
    }
  }

  private def bodyOf(request: Request[AnyContent]): Option[String] = {
    request.body match {
      case AnyContentAsJson(json) => Some(Json.stringify(json))
      case AnyContentAsText(txt) => Some(txt)
      case AnyContentAsXml(xml) => Some(xml.toString())
      case _ => None
    }
  }
}
