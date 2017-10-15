package connectors

import javax.inject.Inject

import config.AppContext
import models.ApiDefinition
import models.GatewayError.ApiNotFound
import models.JsonFormatters._
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiDefinitionConnector @Inject()(appContext: AppContext, wsClient: WSClient) {

  val serviceUrl = appContext.serviceUrl("api-definition")

  def fetchByContext(context: String): Future[ApiDefinition] = {
    wsClient.url(s"$serviceUrl/api-definition?context=$context").get() map {
      case response if response.status == Status.OK => Json.parse(response.body).as[ApiDefinition]
      case response if response.status == Status.NOT_FOUND => throw ApiNotFound()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from api-definition ${r.status} ${r.body}")
    }
  }
}
