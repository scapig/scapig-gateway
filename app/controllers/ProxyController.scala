package controllers

import javax.inject.{Inject, Singleton}

import play.api.mvc._
import services.ProxyService
import services.routing.RoutingService

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ProxyController @Inject()(controllerComponent: ControllerComponents, proxyService: ProxyService, routingService: RoutingService)
  extends AbstractController(controllerComponent){

  def proxy() = Action.async { implicit request =>
    routingService.routeRequest(request) flatMap { apiRequest =>
      proxyService.proxy(request, apiRequest)
    }
  }

}
