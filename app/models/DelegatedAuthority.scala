package models

import org.joda.time.DateTime


case class DelegatedAuthority(clientId: String, userId: String, environment: Environment.Environment, expiresAt: DateTime, token: Token)

case class Token(expiresAt: DateTime,
                 scopes: Set[String])

object Environment extends Enumeration {
  type Environment = Value
  val PRODUCTION, SANDBOX = Value
}
