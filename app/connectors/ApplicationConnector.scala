package connectors

import javax.inject.Inject

import config.AppContext
import models._
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import models.JsonFormatters._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationConnector @Inject()(appContext: AppContext, wsClient: WSClient) {

  val serviceUrl = appContext.serviceUrl("application")

  def fetchByServerToken(serverToken: String): Future[EnvironmentApplication] = {
    wsClient.url(s"$serviceUrl/application?serverToken=$serverToken").get() map {
      case response if response.status == 200 => Json.parse(response.body).as[EnvironmentApplication]
      case response if response.status == 404 => throw ApplicationNotFoundException()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from application ${r.status} ${r.body}")
    }
  }

  def fetchByClientId(clientId: String): Future[EnvironmentApplication] = {
    wsClient.url(s"$serviceUrl/application?clientId=$clientId").get() map {
      case response if response.status == 200 => Json.parse(response.body).as[EnvironmentApplication]
      case response if response.status == 404 => throw ApplicationNotFoundException()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from application ${r.status} ${r.body}")
    }
  }

  def validateSubscription(appId: String,  api: ApiIdentifier): Future[HasSucceeded] = {
    wsClient.url(s"$serviceUrl/application/$appId/${api.context}/${api.version}").get() map {
      case response if response.status == 204 => HasSucceeded
      case response if response.status == 404 => throw SubscriptionNotFoundException()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from application ${r.status} ${r.body}")
    }
  }

}
