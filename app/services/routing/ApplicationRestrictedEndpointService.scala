package services.routing

import javax.inject.{Inject, Singleton}

import models.GatewayError.InvalidCredentials
import models.{ApiRequest, ApplicationNotFoundException, DelegatedAuthorityNotFoundException, ProxyRequest}
import play.api.Logger
import play.api.mvc.{AnyContent, Request}
import services.{ApplicationService, DelegatedAuthorityService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ApplicationRestrictedEndpointService @Inject()(delegatedAuthorityService: DelegatedAuthorityService,
                                                     applicationService: ApplicationService) {

  def routeRequest(request: Request[AnyContent], proxyRequest: ProxyRequest, apiRequest: ApiRequest): Future[ApiRequest] = {

    def getAuthority(accessToken: String) = {
      delegatedAuthorityService.fetchDelegatedAuthority(request, accessToken, apiRequest) recover {
        case e: DelegatedAuthorityNotFoundException =>
          Logger.debug("No valid authority found for the access token")
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
        case e: ApplicationNotFoundException => getApplicationByAuthority(accessToken)
      }
    }

    for {
      accessToken <- proxyRequest.accessToken(request, apiRequest)
      app <- getApplication(accessToken)
      _ <- applicationService.validateSubscriptionAndRateLimit(app, apiRequest.apiIdentifier)
    } yield apiRequest.copy(clientId = Some(app.clientId))
  }

}
