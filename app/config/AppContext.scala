package config

import javax.inject.{Inject, Singleton}

import play.api.Configuration

@Singleton
class AppContext @Inject()(configuration: Configuration) {

  def serviceUrl(serviceName: String): String = {
    val method = configuration.getOptional[String](s"services.$serviceName.method").getOrElse("http")
    val host = configuration.get[String](s"services.$serviceName.host")
    val port = configuration.get[String](s"services.$serviceName.port")
    s"$method://$host:$port"
  }

  lazy val requestTimeoutInMilliseconds: Int = configuration.get[Int]("requestTimeoutInMilliseconds")
}

