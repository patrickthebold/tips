package dto

import java.time.Instant

import play.api.libs.json.Json

/**
  * Corresponds to JSON objects pertaining to the API.
  */
object DTOs {
  // Requests
  final case class TipRequest(message: String)
  final case class CommentRequest(comment: String)

  // Responses
  final case class NewTipResponse(id: Long)
  final case class NewCommentResponse(id: Long)

  final case class Comment(id: Long, message: String, username: String, created: Instant, modified: Instant)
  final case class CommentHistory(id: Long, versions: Seq[HistoricComment])
  final case class HistoricComment(message: String, username: String, modified: Instant) // Having 'created' in the history would be confusing
  final case class Tip(id: Long, message: String, username: String, comments: Seq[Comment], created: Instant, modified: Instant)
  final case class TipNoComment(id: Long, message: String, username: String, created: Instant, modified: Instant)
  final case class TipHistory(id: Long, versions: Seq[HistoricTip])
  final case class HistoricTip(message: String, username: String, modified: Instant) // Having 'created' in the history would be confusing
  final case class StandAloneComment(id: Long, tipId: Long, message: String, username: String, created: Instant, modified: Instant)

  // Serialization
  implicit val readsTipRequest= Json.reads[TipRequest]
  implicit val readsCommentRequest= Json.reads[CommentRequest]
  implicit val writesNewTipResponse = Json.writes[NewTipResponse]

}
