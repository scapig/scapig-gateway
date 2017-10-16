package services.routing

import javax.inject.{Inject, Singleton}

import connectors.ApiDefinitionConnector
import models.GatewayError.{ApiNotFound, MatchingResourceNotFound}
import models._
import play.api.Logger
import play.api.http.HeaderNames
import play.api.mvc.{AnyContent, Request}
import services.routing.EndpointService.{createAndLogApiRequest, findEndpoint}
import utils.ProxyRequestUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.{failed, successful}

@Singleton
class EndpointService @Inject()(apiDefinitionConnector: ApiDefinitionConnector) {

  def apiRequest(proxyRequest: ProxyRequest, request: Request[AnyContent]) = {
    for {
      context <- validateContext(proxyRequest)
      version <- parseVersion(proxyRequest)
      apiDefinition <- apiDefinitionConnector.fetchByContext(context)
      apiEndpoint <- findEndpoint(proxyRequest, context, version, apiDefinition)
    } yield createAndLogApiRequest(proxyRequest, request, context, version, apiDefinition, apiEndpoint)
  }
}

object EndpointService {

  private def createAndLogApiRequest(proxyRequest: ProxyRequest, request: Request[AnyContent], context: String,
                                     version: String, apiDefinition: ApiDefinition, apiEndpoint: Endpoint) = {
    val apiReq = ApiRequest(
      apiIdentifier = ApiIdentifier(context, version),
      authType = apiEndpoint.authType,
      apiEndpoint = s"${apiDefinition.serviceBaseUrl}${proxyRequest.path.stripPrefix("/" + context)}",
      scope = apiEndpoint.scope
    )

    Logger.debug(s"successful api request match for [${stringify(proxyRequest)}] to [$apiReq]")

    apiReq
  }

  private def findEndpoint(proxyRequest: ProxyRequest, requestContext: String, requestVersion: String, apiDefinition: ApiDefinition) = {

    def filterEndpoint(apiEndpoint: Endpoint): Boolean = {
      apiEndpoint.method.toString == proxyRequest.httpMethod &&
        pathMatchesPattern(apiEndpoint.uriPattern, proxyRequest.rawPath) &&
        queryParametersMatch(proxyRequest.queryParameters, apiEndpoint.queryParameters)
    }

    val apiVersion = apiDefinition.versions.find(_.version == requestVersion)
    val apiEndpoint = apiVersion.flatMap(_.endpoints.find(filterEndpoint))

    (apiVersion, apiEndpoint) match {
      case (None, _) => failed(ApiNotFound())
      case (_, None) => failed(MatchingResourceNotFound())
      case (_, Some(endpoint)) => successful(endpoint)
    }
  }

  private def pathMatchesPattern(uriPattern: String, path: String): Boolean = {
    val pattern = parseUriPattern(uriPattern)
    val pathParts = parsePathParts(path).drop(1)

    pattern.length == pathParts.length && pattern.zip(pathParts).forall {
      case (Variable, _) => true
      case (PathPart(requiredPart), providedPart) => requiredPart == providedPart
    }
  }

  private def queryParametersMatch(queryParameters: Map[String, Seq[String]],
                                   endpointQueryParameters: Seq[Parameter]) = {
    endpointQueryParameters match {
      case configuredParams if configuredParams.exists(_.required) =>
        configuredParams.flatMap(cp => queryParameters.get(cp.name)).flatten.nonEmpty
      case _ => true
    }
  }

  private def parsePathParts(value: String) =
    value.stripPrefix("/").split("/")

  private def parseUriPattern(value: String) =
    parsePathParts(value).map {
      case part if part.startsWith("{") && part.endsWith("}") => Variable
      case part => PathPart(part)
    }

  private def stringify(proxyRequest: ProxyRequest): String =
    s"${proxyRequest.httpMethod} ${proxyRequest.path} ${proxyRequest.getHeader(HeaderNames.ACCEPT).getOrElse("")}"

  sealed trait UriPatternPart

  case object Variable extends UriPatternPart

  case class PathPart(part: String) extends UriPatternPart

}
