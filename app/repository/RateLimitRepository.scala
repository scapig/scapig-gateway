package repository

import javax.inject.{Inject, Singleton}

import models.GatewayError.ThrottledOut
import org.joda.time.DateTime
import org.joda.time.DateTime.now
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.api.ReadPreference
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.JSONCollection
import utils.Time
import models.JsonFormatters._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.sequence

case class RateLimitCounter(clientId: String, minutesSinceEpoch: Long, createdAt: DateTime = now(), count: Int = 1)

@Singleton
class RateLimitRepository @Inject()(val reactiveMongoApi: ReactiveMongoApi) {

  val databaseFuture = reactiveMongoApi.database.map(_.collection[JSONCollection]("rateLimitCounter"))

  val indexes = Seq(
    Index(
      Seq("clientId" -> IndexType.Ascending, "minutesSinceEpoch" -> IndexType.Ascending),
      name = Some("rateLimitCounterIndex"),
      unique = true,
      background = true),
    Index(
      Seq("createdAt" -> IndexType.Ascending),
      name = Some("rateLimitCounterTTLIndex"),
      options = BSONDocument("expireAfterSeconds" -> 60))
  )

  def validateAndIncrement(clientId: String, threshold: Int): Future[Unit] = {
    val minutesSinceEpoch = Time.minutesSinceEpoch()

    def findRateLimitCounter(rateLimitCounterDb: JSONCollection) = {
      rateLimitCounterDb.find(Json.obj(
        "clientId" -> clientId,
        "minutesSinceEpoch" -> minutesSinceEpoch))
        .one[RateLimitCounter](ReadPreference.nearest)
    }

    def incrementRateLimitCounter(rateLimitCounterDb: JSONCollection) = {
      rateLimitCounterDb.update(
        Json.obj("clientId" -> clientId, "minutesSinceEpoch" -> minutesSinceEpoch),
        Json.obj("$inc" -> Json.obj("count" -> 1), "$setOnInsert" -> Json.obj("createdAt" -> Json.toJson(now()))),
        upsert = true)
    }

    for {
      db <- databaseFuture
      counter <- findRateLimitCounter(db)
      result <- counter match {
        case Some(r) if r.count >= threshold => Future.failed(new ThrottledOut)
        case _ => incrementRateLimitCounter(db)
      }
    } yield ()
  }

  def ensureIndexes(): Future[Unit] = {
    for {
      db <- databaseFuture
      _ <- sequence(indexes.map(index => db.indexesManager.create(index)))
    } yield ()
  }

  ensureIndexes()
}
