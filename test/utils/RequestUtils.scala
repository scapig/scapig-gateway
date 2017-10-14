package utils

import org.scalatest.Matchers

trait RequestUtils extends Matchers {

  def flattenHeaders(headers: Map[String, Seq[String]]) = headers.mapValues(_.distinct.head)

  def validateHeaders(headers: Map[String, String], expectedHeaders: (String, Option[String])*) =
    expectedHeaders.foreach(x => headers.get(x._1) shouldBe x._2)

}
