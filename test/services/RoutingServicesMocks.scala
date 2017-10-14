package services

import java.util.UUID

import models._
import models.AuthType.AuthType
import models.RateLimitTier.BRONZE
import org.joda.time.DateTime
import org.mockito.Matchers.refEq
import org.mockito.Matchers.any
import org.mockito.Mockito._

import scala.concurrent.Future
import scala.concurrent.Future._
import scala.util.Random

trait RoutingServicesMocks {

  protected def generateRandomAuthType(valueToExclude: AuthType): String = {
    var randomAuthType: AuthType = null
    do {
      randomAuthType = AuthType(Random.nextInt(AuthType.maxId))
    } while (randomAuthType == valueToExclude)
    randomAuthType.toString
  }

  protected def mockAuthority(authorityService: DelegatedAuthorityService, gatewayError: GatewayError) =
    when(authorityService.findAuthority(any(), any(), any())).thenReturn(failed(gatewayError))

  protected def mockAuthority(authorityService: DelegatedAuthorityService, authority: DelegatedAuthority) =
    when(authorityService.findAuthority(any(), any(), any())).thenReturn(successful(authority))

  protected def mockScopeValidation(scopeValidationFilter: ScopeValidator, gatewayError: GatewayError) =
    when(scopeValidationFilter.validate(any(classOf[DelegatedAuthority]), any(classOf[Option[String]])))
      .thenReturn(failed(gatewayError))

  protected def mockScopeValidation(scopeValidationFilter: ScopeValidator) =
    when(scopeValidationFilter.validate(any(classOf[DelegatedAuthority]), any(classOf[Option[String]])))
      .thenReturn(successful(()))

  protected def mockApplicationByClientId(applicationService: ApplicationService, clientId: String, gatewayError: GatewayError) =
    when(applicationService.getByClientId(clientId)).thenReturn(failed(gatewayError))

  protected def mockApplicationByClientId(applicationService: ApplicationService, clientId: String, application: EnvironmentApplication) =
    when(applicationService.getByClientId(clientId)).thenReturn(successful(application))

  protected def mockApplicationByServerToken(applicationService: ApplicationService,  serverToken: String, gatewayError: GatewayError) =
    when(applicationService.getByServerToken(serverToken)).thenReturn(failed(gatewayError))

  protected def mockApplicationByServerToken(applicationService: ApplicationService, serverToken: String, application: EnvironmentApplication) =
    when(applicationService.getByServerToken(serverToken)).thenReturn(successful(application))

  protected def mockValidateSubscriptionAndRateLimit(applicationService: ApplicationService, application: EnvironmentApplication, result: Future[Unit]) =
    when(applicationService.validateSubscriptionAndRateLimit(refEq(application), any[ApiIdentifier]())).thenReturn(result)

  protected def anApplication(): EnvironmentApplication =
    EnvironmentApplication(id = UUID.randomUUID(), clientId = "clientId", rateLimitTier = BRONZE)

  protected def validAuthority(): DelegatedAuthority = {
    val token = Token(DateTime.now.plusMinutes(5), Set.empty)
    DelegatedAuthority("clientId", "userOID", Environment.PRODUCTION, token)
  }

}
