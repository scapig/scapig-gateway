package utils

import models.GatewayError.NotFound
import models.ProxyRequest
import play.api.http.HeaderNames

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}
import scala.util.matching.Regex

object ProxyRequestUtils {

  private val parseContext = firstGroup("""\/([^\/]*).*""".r)
  private val parseVersion = firstGroup("""application\/vnd\.(.*)\.(.*)\+.*""".r)
  private val defaultVersion = "1.0"

  def validateContext[T](proxyRequest: ProxyRequest): Future[String] =
    validateOrElse(parseContext(proxyRequest.rawPath), NotFound())

  def parseVersion[T](proxyRequest: ProxyRequest): Future[String] = {
    val acceptHeader: String = proxyRequest.getHeader(HeaderNames.ACCEPT).getOrElse("")
    successful(parseVersion(acceptHeader).getOrElse(defaultVersion))
  }

  private def validateOrElse(maybeString: Option[String], throwable: Throwable): Future[String] =
    maybeString map successful getOrElse failed(throwable)

  private def firstGroup(regex: Regex) = { value: String =>
    regex.unapplySeq(value) flatMap (_.headOption)
  }

}
