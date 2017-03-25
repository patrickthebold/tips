package dto

import java.time.Instant

import play.api.libs.json._
import play.api.mvc.RequestHeader

/**
  * Corresponds to JSON objects pertaining to the API.
  */
object DTOs {
  // Requests
  final case class TipRequest(message: String)
  final case class CommentRequest(comment: String)
  final case class User(username: String, password: String)

  // Responses
  final case class NewTipResponse(override val tipId: Long) extends ResponseWithTipId
  final case class NewCommentResponse(override val commentId: Long) extends ResponseWithCommentId
  final case class Comment(override val commentId: Long, message: String, username: String, created: Instant, modified: Instant) extends ResponseWithCommentId
  final case class CommentHistory(override val commentId: Long, versions: Seq[HistoricComment]) extends ResponseWithCommentId
  final case class Tip(override val tipId: Long, message: String, username: String, comments: Seq[Comment], created: Instant, modified: Instant) extends ResponseWithTipId
  final case class TipNoComment(override val tipId: Long, message: String, username: String, created: Instant, modified: Instant) extends ResponseWithTipId
  final case class TipHistory(override val tipId: Long, versions: Seq[HistoricTip]) extends ResponseWithTipId
  final case class StandAloneComment(override val commentId: Long, tipId: Long, message: String, username: String, created: Instant, modified: Instant) extends ResponseWithTipId with ResponseWithCommentId

  // Helpers
  final case class HistoricComment(message: String, username: String, modified: Instant) // Having 'created' in the history would be confusing
  final case class HistoricTip(message: String, username: String, modified: Instant) // Having 'created' in the history would be confusing

  // Serialization
  implicit val readsTipRequest = Json.reads[TipRequest]
  implicit val readsCommentRequest = Json.reads[CommentRequest]
  implicit val readUser = Json.reads[User]

  implicit val writesHistoricTip = Json.writes[HistoricTip]
  implicit val writesHistoricComment = Json.writes[HistoricComment]

  implicit def writesNewTipResponse(implicit request: RequestHeader) = withTipLocation(Json.writes[NewTipResponse])
  implicit def writesTipNoComment(implicit request: RequestHeader) = withTipLocation(Json.writes[TipNoComment])
  implicit def writesTipHistory(implicit request: RequestHeader) = withTipLocation(Json.writes[TipHistory])
  implicit def writesNewCommentResponse(implicit request: RequestHeader) = withCommentLocation(Json.writes[NewCommentResponse])
  implicit def writesComment(implicit request: RequestHeader) = withCommentLocation(Json.writes[Comment])
  implicit def writesCommentHistory(implicit request: RequestHeader) = withCommentLocation(Json.writes[CommentHistory])
  implicit def writesTip(implicit request: RequestHeader) = withTipLocation(Json.writes[Tip])
  implicit def writesStandAloneComment(implicit request: RequestHeader) = withTipLocation(withCommentLocation(Json.writes[StandAloneComment]))


  // Adding some type safety to reply with URLs.
  trait ResponseWithTipId {
    val tipId: Long
  }

  trait ResponseWithCommentId {
    val commentId: Long
  }

  // denormalizing the ids to URLs
  def withTipLocation[T <: ResponseWithTipId](writer: OWrites[T])(implicit request: RequestHeader): OWrites[T] = {
    new OWrites[T] {
      override def writes(o: T) = {
        val json: JsObject = writer.writes(o)
        json ++ Json.obj(
          "tip_ref" -> JsString(controllers.routes.TipsController.getTip(o.tipId).absoluteURL()),
          "tip_comments_ref" -> JsString(controllers.routes.TipsController.getComments(o.tipId).absoluteURL()),
          "post_new_comment_ref" -> JsString(controllers.routes.TipsController.newComment(o.tipId).absoluteURL()),
          "tip_history_ref" -> JsString(controllers.routes.TipsController.getTipHistory(o.tipId).absoluteURL())
        )
      }
    }
  }

  def withCommentLocation[T <: ResponseWithCommentId](writer: OWrites[T])(implicit request: RequestHeader): OWrites[T] = {
    new OWrites[T] {
      override def writes(o: T) = {
        val json: JsObject = writer.writes(o)
        json ++ Json.obj(
          "comment_ref" -> JsString(controllers.routes.TipsController.getComment(o.commentId).absoluteURL()),
          "comment_history_ref" -> JsString(controllers.routes.TipsController.getCommentHistory(o.commentId).absoluteURL())
        )
      }
    }
  }

}
