package utils

import models.GatewayError.ApiNotFound
import models.ProxyRequest
import play.api.http.HeaderNames

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}
import scala.util.matching.Regex

object ProxyRequestUtils {

  private val parseContext = group("""\/([^\/]*).*""".r, 1)
  private val parseVersion = group("""application\/vnd\.([A-Za-z]*)\.(.*)\+.*""".r, 2)
  private val defaultVersion = "1.0"

  def validateContext[T](proxyRequest: ProxyRequest): Future[String] =
    validateOrElse(parseContext(proxyRequest.rawPath), ApiNotFound())

  def parseVersion[T](proxyRequest: ProxyRequest): Future[String] = {
    val acceptHeader: String = proxyRequest.getHeader(HeaderNames.ACCEPT).getOrElse("")
    successful(parseVersion(acceptHeader).getOrElse(defaultVersion))
  }

  private def validateOrElse(maybeString: Option[String], throwable: Throwable): Future[String] =
    maybeString map successful getOrElse failed(throwable)

  private def group(regex: Regex, groupNumber: Int) = { value: String =>
    regex.unapplySeq(value) flatMap (s => s.lift(groupNumber - 1))
  }

}
