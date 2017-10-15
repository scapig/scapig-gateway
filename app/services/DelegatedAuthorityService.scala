package services

import javax.inject.{Inject, Singleton}

import connectors.DelegatedAuthorityConnector
import models.{ApiRequest, DelegatedAuthority, DelegatedAuthorityNotFoundException, ProxyRequest}
import models.GatewayError.InvalidCredentials
import org.joda.time.DateTime.now
import play.api.mvc.{AnyContent, Request}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class DelegatedAuthorityService @Inject()(delegatedAuthorityConnector: DelegatedAuthorityConnector) {

  def fetchDelegatedAuthority(request: Request[AnyContent], accessToken: String, apiRequest: ApiRequest): Future[DelegatedAuthority] = {

    def hasExpired(delegatedAuthority: DelegatedAuthority) = delegatedAuthority.token.expiresAt.isBefore(now)

    def validateAuthority(delegatedAuthority: DelegatedAuthority) = {
      if (hasExpired(delegatedAuthority))
        throw DelegatedAuthorityNotFoundException()
      else
        delegatedAuthority
    }

    for {
      authority <- delegatedAuthorityConnector.fetchByAccessToken(accessToken)
    } yield validateAuthority(authority)
  }
}
