import play.api.http.HttpErrorHandler
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent._
import javax.inject.Singleton

import play.api.libs.json.Json

/**
  * No html responses.
  */
@Singleton
class ErrorHandler extends HttpErrorHandler {

  private val logger =
    org.slf4j.LoggerFactory.getLogger(getClass)

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
