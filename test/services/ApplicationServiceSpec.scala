package services

import java.util.UUID

import config.AppContext
import connectors.ApplicationConnector
import models.GatewayError.{InvalidSubscription, ThrottledOut}
import models.RateLimitTier.SILVER
import models._
import org.mockito.BDDMockito.given
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import repository.RateLimitRepository
import utils.UnitSpec

import scala.concurrent.Future._

class ApplicationServiceSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val serverToken = "serverToken"
    val applicationId = UUID.randomUUID()
    val clientId = "clientId"
    val application = EnvironmentApplication(id = applicationId, clientId = "clientId", rateLimitTier = RateLimitTier.BRONZE)
    val bronzeRateLimit = 5
    val silverRateLimit = 10

    val api = ApiIdentifier("aContext", "aVersion")

    val applicationConnector = mock[ApplicationConnector]
    val rateLimitRepository = mock[RateLimitRepository]
    val appContext = mock[AppContext]
    val applicationService = new ApplicationService(applicationConnector, rateLimitRepository, appContext)

    given(appContext.rateLimitBronze).willReturn(bronzeRateLimit)
    given(appContext.rateLimitSilver).willReturn(silverRateLimit)
  }

  "getByServerToken" should {

    "return the application when an application exists for the given server token" in new Setup {
      when(applicationConnector.fetchByServerToken(serverToken)).thenReturn(successful(application))
      val result = await(applicationService.getByServerToken(serverToken))
      result shouldBe application
    }

    "propagate the ApplicationNotFoundException error when the application cannot be fetched for the given server token" in new Setup {
      when(applicationConnector.fetchByServerToken(serverToken)).thenReturn(failed(ApplicationNotFoundException()))
      intercept[ApplicationNotFoundException] {
        await(applicationService.getByServerToken(serverToken))
      }
    }
  }

  "getByClientId" should {

    "return the application when an application exists for the given client id" in new Setup {
      when(applicationConnector.fetchByClientId(clientId)).thenReturn(successful(application))
      val result = await(applicationService.getByClientId(clientId))
      result shouldBe application
    }

    "propagate the ApplicationNotFoundException error when the application cannot be fetched for the given clientId" in new Setup {
      when(applicationConnector.fetchByClientId(clientId)).thenReturn(failed(new RuntimeException))
      intercept[RuntimeException] {
        await(applicationService.getByClientId(clientId))
      }
    }
  }

  "validateSubscriptionAndRateLimit" should {

    "propagate the InvalidSubscription when the application is not subscribed" in new Setup {
      when(applicationConnector.validateSubscription(applicationId.toString, api)).thenReturn(failed(InvalidSubscription()))
      intercept[InvalidSubscription] {
        await(applicationService.validateSubscriptionAndRateLimit(application, api))
      }
    }

    "propagate the ThrottledOut error when the rate limit is reached" in new Setup {
      val silverApplication = application.copy(rateLimitTier = SILVER)

      mockSubscription(applicationConnector, application.id, api)
      given(rateLimitRepository.validateAndIncrement(silverApplication.clientId, silverRateLimit)).willReturn(failed(ThrottledOut()))

      intercept[ThrottledOut] {
        await(applicationService.validateSubscriptionAndRateLimit(silverApplication, api))
      }
    }

    "return successfully when the application is subscribed and the rate limit is not reached" in new Setup {
      mockSubscription(applicationConnector, application.id, api)
      given(rateLimitRepository.validateAndIncrement(application.clientId, bronzeRateLimit)).willReturn(successful(()))

      await(applicationService.validateSubscriptionAndRateLimit(application, api))
    }

  }

  private def mockSubscription(applicationConnector: ApplicationConnector, applicationId: UUID, api: ApiIdentifier) =
    when(applicationConnector.validateSubscription(applicationId.toString, api)).thenReturn(successful(HasSucceeded))

}
