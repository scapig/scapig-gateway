package models

import java.util.UUID

case class Application(rateLimitTier: Option[RateLimitTier.Value],
                       id: UUID)

object RateLimitTier extends Enumeration {
  type RateLimitTier = Value
  val GOLD, SILVER, BRONZE = Value
}
