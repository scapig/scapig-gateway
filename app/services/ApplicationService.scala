package services

import javax.inject.{Inject, Singleton}

import config.AppContext
import connectors.ApplicationConnector
import models.{ApiIdentifier, ApplicationNotFoundException, EnvironmentApplication, RateLimitTier}
import models.GatewayError.ServerError
import play.api.Logger
import repository.RateLimitRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ApplicationService @Inject()(applicationConnector: ApplicationConnector,
                                   rateLimitRepository: RateLimitRepository,
                                   appContext: AppContext) {

  def getByServerToken(serverToken: String): Future[EnvironmentApplication] = {
    applicationConnector.fetchByServerToken(serverToken)
  }

  def getByClientId(clientId: String): Future[EnvironmentApplication] = {
    applicationConnector.fetchByClientId(clientId)
  }

  def validateSubscriptionAndRateLimit(application: EnvironmentApplication, requestedApi: ApiIdentifier): Future[Unit] = {
    val validateSubscription = applicationConnector.validateSubscription(application.id.toString, requestedApi)
    val validateRateLimit = validateApplicationRateLimit(application)

    for {
      _ <- validateSubscription
      _ <- validateRateLimit
    } yield ()
  }

  private def validateApplicationRateLimit(application: EnvironmentApplication): Future[Unit] = {
    rateLimitRepository.validateAndIncrement(application.clientId, rateLimit(application))
  }

  private def rateLimit(application: EnvironmentApplication): Int = {
    application.rateLimitTier match {
      case RateLimitTier.GOLD => appContext.rateLimitGold
      case RateLimitTier.SILVER => appContext.rateLimitSilver
      case _ => appContext.rateLimitBronze
    }
  }
}
