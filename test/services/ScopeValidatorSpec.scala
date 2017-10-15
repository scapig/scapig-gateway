package services

import models.GatewayError.InvalidScope
import models.{DelegatedAuthority, Token}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import utils.UnitSpec

class ScopeValidatorSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val delegatedAuthority = mock[DelegatedAuthority]
    val token = mock[Token]
    val scopeValidator = new ScopeValidator

    when(delegatedAuthority.token).thenReturn(token)
    when(delegatedAuthority.token.scopes).thenReturn(Set("read:scope", "write:scope", "read:another-scope"))
  }

  "validate" should {

    "throw InvalidScope exception when the request has no scopes" in new Setup {
      intercept[InvalidScope] {
        await(scopeValidator.validate(delegatedAuthority, None))
      }
    }

    "throw InvalidScope exception when the request scope is empty" in new Setup {
      intercept[InvalidScope] {
        await(scopeValidator.validate(delegatedAuthority, Some("")))
      }
    }

    "throw InvalidScope exception when the request contains multiple scopes" in new Setup {
      intercept[InvalidScope] {
        await(scopeValidator.validate(delegatedAuthority, Some("read:scope write:scope")))
      }
    }

    "throw InvalidScope exception when the request does not have any of the required scopes" in new Setup {
      intercept[InvalidScope] {
        await(scopeValidator.validate(delegatedAuthority, Some("read:scope-1")))
      }
    }

    "does not throw any exception when the request has all the required scopes" in new Setup {
      await(scopeValidator.validate(delegatedAuthority, Some("read:scope")))
    }

  }

}
