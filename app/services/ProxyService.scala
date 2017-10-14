package services

import javax.inject.{Inject, Singleton}

import connectors.ProxyConnector
import models.ApiRequest
import play.api.mvc.{AnyContent, Request, Result}

import scala.concurrent.Future

@Singleton
class ProxyService @Inject()(proxyConnector: ProxyConnector) {

  def proxy(request: Request[AnyContent], apiRequest: ApiRequest)(implicit requestId: String): Future[Result] = {
    proxyConnector.proxy(request, apiRequest)
  }

}
