package models

case class DelegatedAuthorityNotFoundException() extends Exception
case class ApplicationNotFoundException() extends Exception

trait HasSucceeded
object HasSucceeded extends HasSucceeded