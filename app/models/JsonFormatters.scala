package models

import org.joda.time.DateTime
import play.api.libs.json._
import repository.RateLimitCounter

object JsonFormatters {
  val datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

  implicit val dateRead: Reads[DateTime] = JodaReads.jodaDateReads(datePattern)
  implicit val dateWrite: Writes[DateTime] = JodaWrites.jodaDateWrites(datePattern)
  implicit val dateFormat: Format[DateTime] = Format[DateTime](dateRead, dateWrite)

  implicit val gatewayErrorWrites = Writes[GatewayError] { gatewayError =>
    JsObject(Seq(
      "code" -> JsString(gatewayError.code),
      "message" -> JsString(gatewayError.message)
    ))
  }

  implicit val formatAPIStatus = EnumJson.enumFormat(APIStatus)
  implicit val formatAuthType = EnumJson.enumFormat(AuthType)
  implicit val formatHttpMethod = EnumJson.enumFormat(HttpMethod)
  implicit val formatEnvironment = EnumJson.enumFormat(Environment)
  implicit val formatRateLimitTier = EnumJson.enumFormat(RateLimitTier)

  implicit val formatParameter = Json.format[Parameter]
  implicit val formatEndpoint = Json.format[Endpoint]
  implicit val formatAPIVersion = Json.format[APIVersion]
  implicit val formatApiDefinition = Json.format[ApiDefinition]

  implicit val formatToken = Json.format[Token]
  implicit val formatDelegatedAuthority = Json.format[DelegatedAuthority]

  implicit val formatEnvironmentApplication = Json.format[EnvironmentApplication]

  implicit val formatRateLimitCounter = Json.format[RateLimitCounter]
}
