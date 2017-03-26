package service

import java.sql.{Connection, ResultSet, Statement}
import javax.inject.{Inject, Singleton}

import dto.DTOs._
import play.api.mvc.Results._

import scala.concurrent.Future

/**
  * Interact with database
  * Warning: Lots of mutable state working with Java APIs!
  */
@Singleton
class TipsService @Inject()(db: AsyncDatabase) {

  private val getTipsQuery =
    """
      |SELECT id, message, username, created, modified FROM tips ORDER BY modified DESC;
    """.stripMargin

  private val newTipQuery =
    """
      |INSERT INTO tips VALUES (DEFAULT, ?, ?, DEFAULT, DEFAULT);
    """.stripMargin

  private val getTipQuery =
    """
      |SELECT t.message, t.username, t.created, t.modified, c.id, c.comment, c.username, c.created, c.modified
      |FROM tips t LEFT OUTER JOIN comments c
      |ON c.tip_id = t.id
      |WHERE t.id = ?
      |ORDER BY c.modified DESC;
    """.stripMargin

  private val getTipNoCommentQuery =
    """
      |SELECT message, username, created, modified FROM tips
      |WHERE id = ?;
    """.stripMargin

  private val updateTipQuery =
    """
      |UPDATE tips SET message = ?, username =  ?
      |WHERE id = ?;
    """.stripMargin

  private val getCommentsQuery =
    """
      |SELECT c.id, c.comment, c.username, c.created, c.modified FROM tips t LEFT OUTER JOIN comments c
      |ON c.tip_id = t.id
      |WHERE t.id = ? ORDER BY c.modified DESC;
    """.stripMargin

  private val newCommentQuery =
    """
      |INSERT INTO comments VALUES (DEFAULT, ?, ?, ?, DEFAULT, DEFAULT);
    """.stripMargin

  private val getTipHistoryQuery =
    """
      |SELECT h.message, h.username, h.modified FROM tips t LEFT OUTER JOIN tips_history h
      |ON t.id = h.id
      |WHERE t.id = ?
      |ORDER BY h.modified DESC;
    """.stripMargin

  private val getCommentQuery =
    """
      |SELECT tip_id, comment, username, created, modified FROM comments
      |WHERE id = ?;
    """.stripMargin

  private val getCommentHistoryQuery =
    """
      |SELECT h.comment, h.username, h.modified FROM comments c LEFT OUTER JOIN comments_history h
      |ON c.id = h.id
      |WHERE c.id = ?
      |ORDER BY h.modified DESC;
    """.stripMargin

  private val updateCommentQuery =
    """
      |UPDATE comments SET comment = ?, username = ?
      |WHERE id = ?;
    """.stripMargin

  private val tipExistsQuery =
    """
      |SELECT 1 FROM tips
      |where id = ?;
    """.stripMargin

  private val commentExistsQuery =
    """
      |SELECT 1 FROM comments
      |where id = ?;
    """.stripMargin

  private def tipExists(id: Long, connection: Connection): Boolean = {
    val stmt = connection.prepareStatement(tipExistsQuery)
    stmt.setLong(1, id)
    val results = stmt.executeQuery()
    results.next()
  }

  private def commentExists(id: Long, connection: Connection): Boolean = {
    val stmt = connection.prepareStatement(commentExistsQuery)
    stmt.setLong(1, id)
    val results = stmt.executeQuery()
    results.next()
  }

  // This pulls everything into memory. Will need a better approach.
  private def getTipsImpl(connection: Connection): Seq[TipNoComment] = {
    val stmt = connection.prepareStatement(getTipsQuery)
    val results = stmt.executeQuery()
    val onFirstRow = false
    makeSeq(results, onFirstRow) { row =>
      TipNoComment(row.getLong(1), row.getString(2), row.getString(3), row.getTimestamp(4).toInstant, row.getTimestamp(5).toInstant)
    }
  }

  private def newTipImpl(tip: TipRequest, username: String)(connection: Connection): NewTipResponse = {
    val stmt = connection.prepareStatement(newTipQuery, Statement.RETURN_GENERATED_KEYS)
    stmt.setString(1, tip.message)
    stmt.setString(2, username)
    stmt.executeUpdate()
    val results = stmt.getGeneratedKeys
    results.next()
    NewTipResponse(results.getLong(1))
  }

  private def getTipImpl(id: Long)(connection: Connection): Option[Tip] = {
    val stmt = connection.prepareStatement(getTipQuery)
    stmt.setLong(1, id)
    val results = stmt.executeQuery()
    if (results.next()) {
      // Things are mutable and we are dealing with a left join.
      val tip = TipNoComment(id, results.getString(1), results.getString(2), results.getTimestamp(3).toInstant, results.getTimestamp(4).toInstant)
      results.getLong(5)
      val comments = if (results.wasNull) {
        Seq()
      } else {
        val onFirstRow = true
        makeSeq(results, onFirstRow) {
          row => Comment(results.getLong(5), results.getString(6), results.getString(7), results.getTimestamp(8).toInstant, results.getTimestamp(9).toInstant)
        }
      }
      Some(Tip(tip.tipId, tip.message, tip.username, comments, tip.created, tip.modified))
    } else {
      None
    }
  }

  private def getTipNoCommentImpl(id: Long)(connection: Connection): Option[TipNoComment] = {
    val stmt = connection.prepareStatement(getTipQuery)
    stmt.setLong(1, id)
    val results = stmt.executeQuery()
    if (results.next()) {
      Some(TipNoComment(id, results.getString(1), results.getString(2), results.getTimestamp(3).toInstant, results.getTimestamp(4).toInstant))
    } else {
      None
    }
  }

  private def updateTipImpl(id: Long, message: TipRequest, username: String)(connection: Connection): Status = {
    val stmt = connection.prepareStatement(updateTipQuery)
    stmt.setString(1, message.message)
    stmt.setString(2, username)
    stmt.setLong(3, id)
    val numUpdated = stmt.executeUpdate()
    assert(numUpdated <= 1, s"We expected to update at most one tip record in the database but $numUpdated were updated!")
    if (numUpdated == 0) {
      // check if it's because we are trying to edit with a different user or if it doesn't exist.
      if (tipExists(id, connection)) {
        Forbidden
      } else {
        NotFound
      }
    } else {
      Ok
    }
  }

  private def getCommentsImpl(id: Long)(connection: Connection): Option[Seq[Comment]] = {
    val stmt = connection.prepareStatement(getCommentsQuery)
    stmt.setLong(1, id)
    val results = stmt.executeQuery()
    if (results.next()) {
      // Another Left Join
      results.getLong(1)
      val comments = if (results.wasNull) {
        Seq()
      } else {
        val onFirstRow = true
        makeSeq(results, onFirstRow) {
          row => Comment(results.getLong(1), results.getString(2), results.getString(3), results.getTimestamp(4).toInstant, results.getTimestamp(5).toInstant)
        }
      }
      Some(comments)
    } else {
      None
    }
  }

  private def newCommentImpl(id: Long, comment: CommentRequest, username: String)(connection: Connection): Option[NewCommentResponse] = {
    if (tipExists(id, connection)) {
      val stmt = connection.prepareStatement(newCommentQuery, Statement.RETURN_GENERATED_KEYS)
      stmt.setLong(1, id)
      stmt.setString(2, comment.comment)
      stmt.setString(3, username)
      stmt.execute()
      val results = stmt.getGeneratedKeys
      results.next()
      Some(NewCommentResponse(results.getLong(1)))
    } else {
      None
    }
  }
  private def getTipHistoryImpl(id: Long)(connection: Connection): Option[TipHistory]  = {
    val stmt = connection.prepareStatement(getTipHistoryQuery)
    stmt.setLong(1, id)
    val results = stmt.executeQuery()
    if (results.next()) {
      results.getString(1)
      val historicTips = if (results.wasNull()) {
        Seq()
      } else {
        val onFirstRow = true
        makeSeq(results, onFirstRow) {
          results => HistoricTip(results.getString(1), results.getString(2), results.getTimestamp(3).toInstant)
        }
      }
      Some(TipHistory(id, historicTips))
    } else {
      None
    }
  }

  private def getCommentImpl(id: Long)(connection: Connection): Option[StandAloneComment] = {
    val stmt = connection.prepareStatement(getCommentQuery)
    stmt.setLong(1, id)
    val results = stmt.executeQuery()
    if (results.next()) {
      Some(StandAloneComment(id, results.getLong(1), results.getString(2), results.getString(3), results.getTimestamp(4).toInstant, results.getTimestamp(5).toInstant))
    } else {
      None
    }
  }
  private def getCommentHistoryImpl(id: Long)(connection: Connection): Option[CommentHistory] = {
    val stmt = connection.prepareStatement(getCommentHistoryQuery)
    stmt.setLong(1, id)
    val results = stmt.executeQuery()
    if (results.next()) {
      results.getString(1)
      val historicComments = if (results.wasNull()) {
        Seq()
      } else {
        val onFirstRow = true
        makeSeq(results, onFirstRow) {
          results => HistoricComment(results.getString(1), results.getString(2), results.getTimestamp(3).toInstant)
        }
      }
      Some(CommentHistory(id, historicComments))
    } else {
      None
    }
  }

  private def updateCommentImpl(id: Long, comment: CommentRequest, username: String)(connection: Connection): Status = {
    val stmt = connection.prepareStatement(updateCommentQuery)
    stmt.setString(1, comment.comment)
    stmt.setString(2, username)
    stmt.setLong(3, id)
    val numUpdated = stmt.executeUpdate()
    assert(numUpdated <= 1, s"We expected to update at most one comment record in the database but $numUpdated were updated!")
    if (numUpdated == 0) {
      // check if it's because we are trying to edit with a different user or if it doesn't exist.
      if (commentExists(id, connection)) {
        Forbidden
      } else {
        NotFound
      }
    } else {
      Ok
    }
  }


  def getTips: Future[Seq[TipNoComment]] = db.async(getTipsImpl)
  def newTip(tip: TipRequest, username: String): Future[NewTipResponse] = db.async(newTipImpl(tip, username))

  def getTip(id: Long): Future[Option[Tip]] = db.async(getTipImpl(id))
  def getTipNoComment(id: Long): Future[Option[TipNoComment]] = db.async(getTipNoCommentImpl(id))
  def updateTip(id: Long, message: TipRequest, username: String): Future[Status] = db.async(updateTipImpl(id, message, username))
  def getComments(id: Long): Future[Option[Seq[Comment]]] = db.async(getCommentsImpl(id))
  def newComment(id: Long, comment: CommentRequest, username: String): Future[Option[NewCommentResponse]] = db.async(newCommentImpl(id, comment, username))
  def getTipHistory(id: Long): Future[Option[TipHistory]]  = db.async(getTipHistoryImpl(id))

  def getComment(id: Long): Future[Option[StandAloneComment]] = db.async(getCommentImpl(id))
  def getCommentHistory(id: Long): Future[Option[CommentHistory]] = db.async(getCommentHistoryImpl(id))
  def updateComment(id: Long, comment: CommentRequest, username: String): Future[Status] = db.async(updateCommentImpl(id, comment, username))

  // helper
  private def makeSeq[T](results: ResultSet, onFirstRow: Boolean)(f: ResultSet => T): Seq[T] = {
    val sb =  Seq.newBuilder[T]
    if (onFirstRow) {
      sb += f(results)
    }
    while (results.next()) {
      sb += f(results)
    }
    sb.result()
  }

}
