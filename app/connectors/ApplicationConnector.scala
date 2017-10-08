package connectors

import javax.inject.Inject

import config.AppContext
import models.{Application, ApplicationNotFoundException}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import models.JsonFormatters._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class ApplicationConnector @Inject()(appContext: AppContext, wsClient: WSClient) {

  val serviceUrl = appContext.serviceUrl("application")

  def fetchByServerToken(serverToken: String): Future[Application] = {
    wsClient.url(s"$serviceUrl/application?serverToken=$serverToken").get() map {
      case response if response.status == 200 => Json.parse(response.body).as[Application]
      case response if response.status == 404 => throw ApplicationNotFoundException()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from application ${r.status} ${r.body}")
    }
  }

  def fetchByClientId(clientId: String): Future[Application] = {
    wsClient.url(s"$serviceUrl/application?clientId=$clientId").get() map {
      case response if response.status == 200 => Json.parse(response.body).as[Application]
      case response if response.status == 404 => throw ApplicationNotFoundException()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from application ${r.status} ${r.body}")
    }
  }

}
