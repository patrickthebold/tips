import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent.Future
import javax.inject.Singleton

import play.api.libs.json.Json

import util.Logging

/**
  * No html responses.
  */
@Singleton
class ErrorHandler extends HttpErrorHandler with Logging {


  def onClientError(request: RequestHeader, statusCode: Int, message: String) = {
    logger.info("Returning {} due to {}", statusCode, message)
    Future.successful(
      Status(statusCode)(Json.obj("message" -> message))
    )
  }

  def onServerError(request: RequestHeader, exception: Throwable) = {
    logger.error("Exception: ", exception)
    Future.successful(
      InternalServerError
    )
  }
}
