package play

import java.util.UUID
import javax.inject.{Inject, Singleton}

import _root_.controllers.ProxyController
import play.api.http.{DefaultHttpRequestHandler, HttpConfiguration, HttpErrorHandler, HttpFilters}
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router

@Singleton
class ProxyRequestHandler @Inject()
(errorHandler: HttpErrorHandler,
 configuration: HttpConfiguration,
 filters: HttpFilters,
 proxyRoutes: Router,
 proxyController: ProxyController)
  extends DefaultHttpRequestHandler(proxyRoutes, errorHandler, configuration, filters) {

  override def handlerForRequest(requestHeader: RequestHeader): (RequestHeader, Handler) = {
     super.handlerForRequest(requestHeader)
  }

  override def routeRequest(requestHeader: RequestHeader): Option[Handler] = {
    implicit val requestId = UUID.randomUUID().toString
    Some(proxyController.proxy)
  }
}
