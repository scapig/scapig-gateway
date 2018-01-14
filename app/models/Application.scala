package models

import java.util.UUID

import models.Environment.Environment

case class EnvironmentApplication(id: UUID,
                                  clientId: String,
                                  environment: Environment,
                                  rateLimitTier: RateLimitTier.Value
                                  )

object RateLimitTier extends Enumeration {
  type RateLimitTier = Value
  val GOLD, SILVER, BRONZE = Value
}
