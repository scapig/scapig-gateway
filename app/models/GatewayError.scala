package models

import play.api.mvc.{AnyContent, Request}

class GatewayError(val code: String, val message: String) extends RuntimeException(message)

object GatewayError {
  case class ErrorInvalidRequest(errorMessage: String) extends GatewayError("INVALID_REQUEST", errorMessage)

  case class ServiceNotAvailable() extends GatewayError("SERVER_ERROR", "Service unavailable")

  case class ServerError() extends GatewayError("SERVER_ERROR", "Internal server error")

  case class ApiNotFound() extends GatewayError("NOT_FOUND", "The requested resource could not be found.")

  case class MatchingResourceNotFound() extends GatewayError("MATCHING_RESOURCE_NOT_FOUND", "A resource with the name in the request cannot be found in the API")

  case class InvalidCredentials(request: Request[AnyContent], apiRequest: ApiRequest) extends GatewayError("INVALID_CREDENTIALS", "Invalid Authentication information provided")

  case class MissingCredentials(request: Request[AnyContent], apiRequest: ApiRequest) extends GatewayError("MISSING_CREDENTIALS", "Authentication information is not provided")

  case class InvalidScope() extends GatewayError("INVALID_SCOPE", "Cannot access the required resource. Ensure this token has all the required scopes.")

  case class InvalidSubscription() extends GatewayError("RESOURCE_FORBIDDEN", "The application is not subscribed to the API which it is attempting to invoke")

  case class ThrottledOut() extends GatewayError("MESSAGE_THROTTLED_OUT", "The request for the API is throttled as you have exceeded your quota.")

}
