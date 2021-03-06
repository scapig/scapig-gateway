package connectors

import javax.inject.Inject

import config.AppContext
import models.{DelegatedAuthority, DelegatedAuthorityNotFoundException}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import models.JsonFormatters._
import play.api.http.Status

import scala.concurrent.Future

class DelegatedAuthorityConnector @Inject()(appContext: AppContext, wsClient: WSClient) {

  val serviceUrl = appContext.serviceUrl("delegated-authority")

  def fetchByAccessToken(accessToken: String): Future[DelegatedAuthority] = {
    wsClient.url(s"$serviceUrl/authority?accessToken=$accessToken").get() map {
      case response if response.status == Status.OK => Json.parse(response.body).as[DelegatedAuthority]
      case response if response.status == Status.NOT_FOUND => throw DelegatedAuthorityNotFoundException()
      case r: WSResponse => throw new RuntimeException(s"Invalid response from delegated-authority ${r.status} ${r.body}")
    }
  }
}
