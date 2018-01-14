package models

import java.util.UUID

import models.AuthType.AuthType
import models.Environment.Environment


case class ApiRequest(apiIdentifier: ApiIdentifier,
                      serviceBaseUrl: String,
                      path: String,
                      authType: AuthType,
                      environment: Option[Environment] = None,
                      scope: Option[String] = None,
                      userId: Option[String] = None,
                      clientId: Option[String] = None,
                      requestId: UUID = UUID.randomUUID()) {

  lazy val apiEndpoint = {
    environment match {
      case Some(Environment.SANDBOX) => s"$serviceBaseUrl/sandbox$path"
      case _ => s"$serviceBaseUrl$path"
    }
  }
}

case class ApiIdentifier(context: String, version: String)
