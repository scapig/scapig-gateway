package services

import javax.inject.Singleton

import models.DelegatedAuthority
import models.GatewayError.InvalidScope

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

@Singleton
class ScopeValidator {

  def validate(delegatedAuthority: DelegatedAuthority, maybeScope: Option[String]): Future[Unit] =
    maybeScope match {
      case Some(scope) if delegatedAuthority.token.scopes.contains(scope) => successful(())
      case _ => failed(InvalidScope())
    }
}
