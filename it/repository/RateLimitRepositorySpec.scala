package repository

import models.GatewayError.ThrottledOut
import org.joda.time.DateTime.now
import org.joda.time.DateTimeUtils
import org.joda.time.DateTimeUtils.setCurrentMillisFixed
import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import utils.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class RateLimitRepositorySpec extends UnitSpec with BeforeAndAfterEach {

  lazy val fakeApplication = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> "mongodb://localhost:27017/scapig-gateway-test")
    .build()

  val underTest = fakeApplication.injector.instanceOf[RateLimitRepository]

  override def beforeEach(): Unit = {
    await(underTest.ensureIndexes())
    setCurrentMillisFixed(1000)
  }

  override def afterEach(): Unit = {
    await(await(underTest.databaseFuture).drop(failIfNotFound = false))
    DateTimeUtils.setCurrentMillisSystem()
  }

  "validateAndIncrement" should {

    "return successfully and increment when the threshold is not reached" in {

      val result = await(underTest.validateAndIncrement("clientId", 10))

      result shouldBe ((): Unit)
    }

    "fail when the threshold is reached" in {

      await(underTest.validateAndIncrement("clientId", 2))
      await(underTest.validateAndIncrement("clientId", 2))

      intercept[ThrottledOut] {
        await(underTest.validateAndIncrement("clientId", 2))
      }
    }

    "reset the threshold when a minute has passed" in {

      await(underTest.validateAndIncrement("clientId", 2))
      await(underTest.validateAndIncrement("clientId", 2))
      setCurrentMillisFixed(now().plusMinutes(1).getMillis)

      val result = await(underTest.validateAndIncrement("clientId", 2))

      result shouldBe ((): Unit)
    }
  }
}
