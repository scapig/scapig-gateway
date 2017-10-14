package utils

import play.api.libs.json.Json
import play.api.mvc._

object PlayRequestUtils {

  def bodyOf(request: Request[AnyContent]): Option[String] = {
    request.body match {
      case AnyContentAsJson(json) => Some(Json.stringify(json))
      case AnyContentAsText(txt) => Some(txt)
      case AnyContentAsXml(xml) => Some(xml.toString())
      case _ => None
    }
  }

  def asMapOfSets(seqOfPairs: Seq[(String, String)]): Map[String, Set[String]] =
    seqOfPairs
      .groupBy(_._1)
      .mapValues(_.map(_._2).toSet)

  def replaceHeaders(headers: Headers)(updatedHeaders: (String, Option[String])*): Headers = {
    updatedHeaders.headOption match {
      case Some((headerName, Some(headerValue))) => replaceHeaders(headers.replace(headerName -> headerValue))(updatedHeaders.tail:_*)
      case Some((headerName, None)) => replaceHeaders(headers.remove(headerName))(updatedHeaders.tail:_*)
      case None => headers
    }
  }
}
