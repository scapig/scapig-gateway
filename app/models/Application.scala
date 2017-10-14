package models

import java.util.UUID

case class EnvironmentApplication(id: UUID,
                                  clientId: String,
                                  rateLimitTier: RateLimitTier.Value
                                  )

object RateLimitTier extends Enumeration {
  type RateLimitTier = Value
  val GOLD, SILVER, BRONZE = Value
}
