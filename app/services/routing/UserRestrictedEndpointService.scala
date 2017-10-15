package services.routing

import javax.inject.{Inject, Singleton}

import models.GatewayError.{IncorrectAccessTokenType, InvalidCredentials}
import models.{ApiRequest, ApplicationNotFoundException, DelegatedAuthorityNotFoundException, ProxyRequest}
import play.api.mvc.{AnyContent, Request}
import services.{ApplicationService, DelegatedAuthorityService, ScopeValidator}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UserRestrictedEndpointService @Inject()(authorityService: DelegatedAuthorityService,
                                              applicationService: ApplicationService,
                                              scopeValidator: ScopeValidator) {

  def routeRequest(request: Request[AnyContent], proxyRequest: ProxyRequest, apiRequest: ApiRequest): Future[ApiRequest] = {

    def getApplicationByServerToken(accessToken: String) = {
      applicationService.getByServerToken(accessToken) recover {
        case e: ApplicationNotFoundException =>
          throw InvalidCredentials(request, apiRequest)
      }
    }

    def getAuthority(accessToken: String) = {
      authorityService.findAuthority(request, accessToken, apiRequest) recoverWith {
        case e: DelegatedAuthorityNotFoundException => getApplicationByServerToken(accessToken).map(_ => throw IncorrectAccessTokenType())
      }
    }

    for {
      accessToken <- proxyRequest.accessToken(request, apiRequest)
      delegatedAuthority <- getAuthority(accessToken)
      application <- applicationService.getByClientId(delegatedAuthority.clientId)
      _ <- applicationService.validateSubscriptionAndRateLimit(application, apiRequest.apiIdentifier)
      _ <- scopeValidator.validate(delegatedAuthority, apiRequest.scope)
    } yield apiRequest.copy(
      userOid = Some(delegatedAuthority.userId),
      clientId = Some(delegatedAuthority.clientId))
  }
}
