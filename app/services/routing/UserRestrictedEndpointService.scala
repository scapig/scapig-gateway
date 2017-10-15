package services.routing

import javax.inject.{Inject, Singleton}

import models.GatewayError.InvalidCredentials
import models.{ApiRequest, DelegatedAuthorityNotFoundException, ProxyRequest}
import play.api.mvc.{AnyContent, Request}
import services.{ApplicationService, DelegatedAuthorityService, ScopeValidator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UserRestrictedEndpointService @Inject()(delegatedAuthorityService: DelegatedAuthorityService,
                                              applicationService: ApplicationService,
                                              scopeValidator: ScopeValidator) {

  def routeRequest(request: Request[AnyContent], proxyRequest: ProxyRequest, apiRequest: ApiRequest): Future[ApiRequest] = {

    def getAuthority(accessToken: String) = {
      delegatedAuthorityService.fetchDelegatedAuthority(request, accessToken, apiRequest) recoverWith {
        case e: DelegatedAuthorityNotFoundException => throw InvalidCredentials(request, apiRequest)
      }
    }

    for {
      accessToken <- proxyRequest.accessToken(request, apiRequest)
      delegatedAuthority <- getAuthority(accessToken)
      application <- applicationService.getByClientId(delegatedAuthority.clientId)
      _ <- applicationService.validateSubscriptionAndRateLimit(application, apiRequest.apiIdentifier)
      _ <- scopeValidator.validate(delegatedAuthority, apiRequest.scope)
    } yield apiRequest.copy(
      userId = Some(delegatedAuthority.userId),
      clientId = Some(delegatedAuthority.clientId))
  }
}
