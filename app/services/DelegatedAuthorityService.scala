package services

import javax.inject.{Inject, Singleton}

import connectors.DelegatedAuthorityConnector
import models.{ApiRequest, DelegatedAuthority, ProxyRequest}
import models.GatewayError.InvalidCredentials
import org.joda.time.DateTime.now
import play.api.mvc.{AnyContent, Request}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class DelegatedAuthorityService @Inject()(delegatedAuthorityConnector: DelegatedAuthorityConnector) {

  def findAuthority(request: Request[AnyContent], proxyRequest: ProxyRequest, apiRequest: ApiRequest): Future[DelegatedAuthority] = {

    def getDelegatedAuthority(proxyRequest: ProxyRequest): Future[DelegatedAuthority] = {
      proxyRequest.accessToken(request, apiRequest).flatMap { accessToken =>
        delegatedAuthorityConnector.fetchByAccessToken(accessToken)
      }
    }

    def hasExpired(delegatedAuthority: DelegatedAuthority) = delegatedAuthority.token.expiresAt.isBefore(now)

    def validateAuthority(delegatedAuthority: DelegatedAuthority) = {
      if (hasExpired(delegatedAuthority))
        throw InvalidCredentials(request, apiRequest)
      else
        delegatedAuthority
    }

    for {
      authority <- getDelegatedAuthority(proxyRequest)
    } yield validateAuthority(authority)
  }
}
