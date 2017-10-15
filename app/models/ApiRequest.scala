package models

import java.util.UUID

import models.AuthType.AuthType


case class ApiRequest(apiIdentifier: ApiIdentifier,
                      authType: AuthType,
                      apiEndpoint: String,
                      scope: Option[String] = None,
                      userId: Option[String] = None,
                      clientId: Option[String] = None,
                      requestId: UUID = UUID.randomUUID())

case class ApiIdentifier(context: String, version: String)
