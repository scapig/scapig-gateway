package utils

import java.nio.charset.Charset

import akka.stream.Materializer
import akka.util.ByteString
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.NoMaterializer

import scala.concurrent.Future.successful
import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}

class UnitSpec extends WordSpec with Matchers {

  def await[T](future: Future[T]): T = Await.result(future, 10.seconds)

  def status(of: Future[Result]): Int = status(Await.result(of, 10.second))
  def status(of: Result): Int = of.header.status

  def jsonBodyOf(result: Result)(implicit mat: Materializer = NoMaterializer): JsValue = {
    Json.parse(bodyOf(result))
  }
  def bodyOf(result: Result)(implicit mat: Materializer = NoMaterializer): String = {
    val bodyBytes: ByteString = await(result.body.consumeData)
    bodyBytes.decodeString(Charset.defaultCharset().name)
  }

  def returnSame[T] = new Answer[Future[T]] {
    override def answer(invocationOnMock: InvocationOnMock): Future[T] = {
      val argument = invocationOnMock.getArguments()(0)
      successful(argument.asInstanceOf[T])
    }
  }

}
