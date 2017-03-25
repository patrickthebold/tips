package controllers

import java.time.Instant
import javax.inject.{Inject, Singleton}

import play.api.Configuration

import play.api.mvc.Results.Forbidden
import play.api.mvc.Security.{AuthenticatedBuilder, AuthenticatedRequest}
import play.api.mvc.{Request, RequestHeader, Result, Session}

import scala.concurrent.Future

/**
  * Play framework cookie expiration doesn't prevent users
  * from storing the cookie and using it past the expiration.
  * https://www.playframework.com/documentation/2.5.x/ScalaSessionFlash
  * That seems a bit silly.
  */
@Singleton
class SessionUtil @Inject()(config: Configuration) {

  private val sessionTimeoutMillis = config.getLong("play.http.session.maxAge").getOrElse(360000L)
  def nextExpiration: (String, String) = ("expires", Instant.now.plusMillis(sessionTimeoutMillis).toString)
  private def setExpiration(session: Session): Session = session + nextExpiration
  private def getUser(request: RequestHeader): Option[String] = {
    request.session.get("expires") map
      Instant.parse filter
      { _.isBefore(Instant.now()) } flatMap
      { _ => request.session.get("username") }
  }

  object Authenticated extends AuthenticatedBuilder[String](getUser, _ => Forbidden) {
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    override def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A, String]) => Future[Result]) = {
      super.invokeBlock(request, block) map { result => result.withSession {
        setExpiration(result.session(request))
      }}
    }
  }

}
