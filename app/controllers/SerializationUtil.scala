package controllers

import play.api.libs.json.{JsError, Json, Reads, Writes}
import play.api.mvc.Results._
import play.api.mvc.{BodyParsers, Result}

import scala.concurrent.Future

object SerializationUtil {


  // (n.b. From the play documentation https://www.playframework.com/documentation/2.5.x/ScalaJsonHttp)
  // This helper parses and validates JSON using the implicit `placeReads`
  // above, returning errors if the parsed json fails validation.
  def validateJson[A : Reads] = BodyParsers.parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

  // (Total Overkill based on current state, but should work for a while.)

  // We have a convention for status codes:
  // Any failed future is a 500 error handled by the ErrorHandler
  // Any Success(None) is a 404
  // Other 4?? status can be returned with an Either[Status,T] or Either[(Status,S),T] -- S is the body in the failed case.
  // Any other Success is serialized as JSON.
  // This can be bypassed because also:
  // Status can be returned directly -- No Body
  // (Status, T) -- Body is serialized from T
  //
  // Overloading is not idiomatic Scala but I think it's best in this case.
  // (May want to lower to not work on futures, but for now everything is a future.)
  def serialize[T](future: Future[T])(implicit writes: Writes[T]):Future[Result] = {
    future map { resp => Ok(Json.toJson(resp)) }
  }

  def serialize[T](future: Future[Option[T]])(implicit writes: Writes[T]):Future[Result] = {
    future map {
      case Some(resp) => Ok(Json.toJson(resp))
      case None => NotFound
    }
  }

  def serialize[T](future: Future[Either[Status,T]])(implicit writes: Writes[T]):Future[Result] = {
    future map {
      case Right(resp) => Ok(Json.toJson(resp))
      case Left(result) => result
    }
  }

  // kind of silly, but for consistency's sake
  def serialize(future: Future[Status]):Future[Result] = {
    future
  }

  def serialize[T](future: Future[(Status,T)])(implicit writes: Writes[T]):Future[Result] = {
    future map {
      case (status, resp) => status(Json.toJson(resp))
    }
  }

  def serialize[T,S](future: Future[Either[(Status,S),T]])(implicit writes: Writes[T], writes2: Writes[S]):Future[Result] = {
    future map {
      case Right(resp) => Ok(Json.toJson(resp))
      case Left((status, resp)) => status(Json.toJson(resp))
    }
  }

  def serialize[T](future: Future[Either[Status,T]])(implicit writes: Writes[T]):Future[Result] = {
    future map {
      case Right(resp) => Ok(Json.toJson(resp))
      case Left(status) => status
    }
  }


}
