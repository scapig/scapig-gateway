package services

import javax.inject.{Inject, Singleton}

import models.{ApiRequest, AuthType, ProxyRequest}
import play.api.mvc.{AnyContent, Request}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

@Singleton
class RoutingService @Inject()(endpointService: EndpointService,
                               userRestrictedEndpointService: UserRestrictedEndpointService,
                               applicationRestrictedEndpointService: ApplicationRestrictedEndpointService) {

  def routeRequest(request: Request[AnyContent]): Future[ApiRequest] = {
    val proxyRequest = ProxyRequest(request)
    val apiRequestF = endpointService.apiRequest(proxyRequest, request)
    apiRequestF flatMap { apiRequest =>
      apiRequest.authType match {
        case AuthType.USER => userRestrictedEndpointService.routeRequest(request, proxyRequest, apiRequest)
        case AuthType.APPLICATION => applicationRestrictedEndpointService.routeRequest(request, proxyRequest, apiRequest)
        case _ => apiRequestF
      }
    }
  }

}
