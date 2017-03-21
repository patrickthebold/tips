package dto

import java.time.Instant

import play.api.libs.json.Json

/**
  * Corresponds to JSON objects pertaining to the API.
  */
object DTOs {
  // Requests
  final case class NewTip(message: String)

  // Responses
  final case class NewTipResponse(id: Long)
  final case class Comment(id: Long, message: String, username: String, created: Instant, modified: Instant)
  final case class Tip(id: Long, message: String, username: String, comments: Seq[Comment], created: Instant, modified: Instant)
  final case class StandAloneComment(id: Long, tipId: Long, message: String, username: String, created: Instant, modified: Instant)

  // Serialization
  implicit val readsNewTip = Json.reads[NewTip]
  implicit val writesNewTipResponse = Json.writes[NewTipResponse]

}
