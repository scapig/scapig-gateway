package services

import javax.inject.{Inject, Singleton}

import models.{ApiRequest, ProxyRequest}
import models.GatewayError.{InvalidCredentials, NotFound}
import play.api.Logger
import play.api.mvc.{AnyContent, Request}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ApplicationRestrictedEndpointService @Inject()(authorityService: DelegatedAuthorityService,
                                                     applicationService: ApplicationService) {

  def routeRequest(request: Request[AnyContent], proxyRequest: ProxyRequest, apiRequest: ApiRequest): Future[ApiRequest] = {

    def getAuthority(accessToken: String) = {
      authorityService.findAuthority(request, proxyRequest, apiRequest) recover {
        case e: NotFound =>
          Logger.debug("No authority found for the access token")
          throw InvalidCredentials(request, apiRequest)
      }
    }

    def getApplicationByAuthority(accessToken: String) = {
      for {
        delegatedAuthority <- getAuthority(accessToken)
        app <- applicationService.getByClientId(delegatedAuthority.clientId)
      } yield app
    }

    def getApplication(accessToken: String) = {
      applicationService.getByServerToken(accessToken) recoverWith {
        case e: NotFound => getApplicationByAuthority(accessToken)
      }
    }

    for {
      accessToken <- proxyRequest.accessToken(request, apiRequest)
      app <- getApplication(accessToken)
      _ <- applicationService.validateSubscriptionAndRateLimit(app, apiRequest.apiIdentifier)
    } yield apiRequest.copy(clientId = Some(app.clientId))
  }

}
