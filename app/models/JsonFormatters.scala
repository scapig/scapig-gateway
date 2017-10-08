package models

import play.api.libs.json.{JsValue, Json, Writes}

object JsonFormatters {

  implicit val errorResponseWrites = new Writes[ErrorResponse] {
    def writes(e: ErrorResponse): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message)
  }
}
