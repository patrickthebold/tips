package controllers

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, Json, Reads, Writes}
import play.api.mvc.Results._
import play.api.mvc.{BodyParsers, Result}

object SerializationUtil {


  // (n.b. From the play documentation https://www.playframework.com/documentation/2.5.x/ScalaJsonHttp)
  // This helper parses and validates JSON using the implicit `placeReads`
  // above, returning errors if the parsed json fails validation.
  def validateJson[A : Reads] = BodyParsers.parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

  // (Total Overkill based on current state, but should work for a while.)

  // We have a convention for status codes:
  // None is a 404
  // Other 4?? status can be returned with an Either[Status,T] or Either[(Status,S),T] -- S is the body in the failed case.
  // Any other values are 200 and serialized as JSON.
  // This can be bypassed because also:
  // Status can be returned directly -- No Body
  // (Status, T) -- Body is serialized from T
  //
  // Overloading is not idiomatic Scala but I think it's best in this case.

  def serialize[T](resp: T)(implicit writes: Writes[T]):Result = {
    Ok(Json.toJson(resp))
  }

  def serialize[T](resp: Option[T])(implicit writes: Writes[T]):Result = {
    resp.fold[Result](NotFound) { body => Ok(Json.toJson(body)) }
  }

  // kind of silly, but for consistency's sake
  def serialize(resp: Status):Result = {
    resp
  }

  def serialize[T](resp: (Status,T))(implicit writes: Writes[T]):Result = {
    resp match {
      case (status, body) => status(Json.toJson(body))
    }
  }

  def serialize[T,S](resp: Either[(Status,S),T])(implicit writes: Writes[T], writes2: Writes[S]):Result = {
    resp match {
      case Right(body) => Ok(Json.toJson(body))
      case Left((status, body)) => status(Json.toJson(body))
    }
  }

  def serialize[T](resp: Either[Status,T])(implicit writes: Writes[T], d: DummyImplicit):Result = {
    resp match {
      case Right(body) => Ok(Json.toJson(body))
      case Left(status) => status
    }
  }

}
