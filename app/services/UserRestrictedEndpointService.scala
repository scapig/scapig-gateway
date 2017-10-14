package services

import javax.inject.{Inject, Singleton}

import models.GatewayError.{IncorrectAccessTokenType, InvalidCredentials, NotFound}
import models.{ApiRequest, ProxyRequest}
import play.api.mvc.{AnyContent, Request}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class UserRestrictedEndpointService @Inject()(authorityService: DelegatedAuthorityService,
                                              applicationService: ApplicationService,
                                              scopeValidator: ScopeValidator) {

  def routeRequest(request: Request[AnyContent], proxyRequest: ProxyRequest, apiRequest: ApiRequest): Future[ApiRequest] = {

    def getApplication(accessToken: String) = {
      applicationService.getByServerToken(accessToken) recover {
        case e: NotFound =>
          throw InvalidCredentials(request, apiRequest)
      }
    }

    def getApplicationByServerToken = {
      proxyRequest.accessToken(request, apiRequest) flatMap getApplication
    }

    def getAuthority = {
      authorityService.findAuthority(request, proxyRequest, apiRequest) recoverWith {
        case e: NotFound => getApplicationByServerToken.map(_ => throw IncorrectAccessTokenType())
      }
    }

    getAuthority flatMap { delegatedAuthority =>
      val validateScopes: Future[Unit] = scopeValidator.validate(delegatedAuthority, apiRequest.scope)

      for {
        application <- applicationService.getByClientId(delegatedAuthority.clientId)
        _ <- applicationService.validateSubscriptionAndRateLimit(application, apiRequest.apiIdentifier)
        _ <- validateScopes
      } yield apiRequest.copy(
        userOid = Some(delegatedAuthority.userId),
        clientId = Some(delegatedAuthority.clientId))
    }
  }

}
