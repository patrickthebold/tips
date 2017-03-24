package service

import java.sql.{Connection, ResultSet, Statement, Timestamp}
import java.time.Instant
import javax.inject.Inject

import dto.DTOs._
import play.api.db.Database
import play.api.mvc.Results._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Interact with database
  * Warning: Lots of mutable state working with Java APIs!
  */
//@Singleton
class TipsService @Inject()(db: Database) {

  private val getTipsQuery =
    """
      |SELECT id, message, username, created, modified FROM tips;
    """.stripMargin

  private val newTipQuery =
    """
      |INSERT INTO tips VALUES (DEFAULT, ?, ?, DEFAULT, DEFAULT);
    """.stripMargin

  private val getTipQuery =
    """
      |SELECT t.message, t.username, t.created, t.modified, c.id, c.comment, c.username, c.created, c.modified
      |FROM tips LEFT OUTER JOIN comments c
      |ON c.tip_id = t.id
      |WHERE id = ?;
    """.stripMargin

  private val getTipNoCommentQuery =
    """
      |SELECT message, username, created, modified FROM tips
      |WHERE id = ?;
    """.stripMargin

  private val updateTipQuery =
    """
      |UPDATE tips SET message = ?
      |WHERE id = ?;
    """.stripMargin

  private val getCommentsQuery =
    """
      |SELECT c.id, c.comment, c.username, c.created, c.modified FROM tips t LEFT OUTER JOIN comments c
      |WHERE tip_id = ?;
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
    """.stripMargin

  private val getCommentQuery =
    """
      |SELECT comment, username, created, modified FROM comments
      |WHERE id = ?;
    """.stripMargin

  private val getCommentHistoryQuery =
    """
      |SELECT h.message, h.username, h.modified FROM comments c LEFT OUTER JOIN comments_history h
      |ON c.id = h.id
      |WHERE c.id = ?
    """.stripMargin

  private val updateCommentQuery =
    """
      |UPDATE comments SET message = ?
      |WHERE id = ?;
    """.stripMargin

  private val updateTipCheckQuery = // Disallow modifications by different users.
    """
      |SELECT t2.id FROM tips t1 LEFT OUTER JOIN tips t2
      |ON t1.id = t2.id AND t2.username = ?
      |WHERE t1.id = ?;
    """.stripMargin

  private val updateCommentCheckQuery = // Disallow modifications by different users.
    """
      |SELECT c2.id FROM commments c1 LEFT OUTER JOIN comments c2
      |ON c1.id = c2.id AND c2.username = ?
      |WHERE c1.id = ?;
    """.stripMargin

  // This pulls everything into memory. Will need a better approach.
  private def getTipsImpl(connection: Connection): Seq[TipNoComment] = {
    val stmt = connection.prepareStatement(getTipsQuery)
    val results = stmt.executeQuery()
    makeSeq(results) { row =>
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
      val firstCommentId = results.getLong(5)
      val comments = if (results.wasNull) {
        Seq()
      } else {
        Comment(firstCommentId, results.getString(6), results.getString(7), results.getTimestamp(8).toInstant, results.getTimestamp(9).toInstant) +: makeSeq(results) {
          row => Comment(results.getLong(5), results.getString(6), results.getString(7), results.getTimestamp(8).toInstant, results.getTimestamp(9).toInstant)
        }
      }
      Some(Tip(tip.id, tip.message, tip.username, comments, tip.created, tip.modified))
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
    val checkStmt = connection.prepareStatement(updateTipCheckQuery)
    checkStmt.setString(1, username)
    checkStmt.setLong(2, id)
    val checkResult = checkStmt.executeQuery()
    if (checkResult.next()) {
      checkResult.getLong(1)
      if (checkResult.wasNull()) {
        Forbidden // different user attempting to modify comment.
      } else {
        val stmt = connection.prepareStatement(updateTipQuery)
        stmt.setString(1, message.message)
        stmt.setLong(2, id)
        val numUpdated = stmt.executeUpdate()
        assert(numUpdated == 1, s"We expected to only update one tip record in the database but $numUpdated were updated!")
        Ok
      }
    } else {
      NotFound
    }
  }

  private def getCommentsImpl(id: Long)(connection: Connection): Option[Seq[Comment]] = {
    val stmt = connection.prepareStatement(getCommentsQuery)
    stmt.setLong(1, id)
    val results = stmt.executeQuery()
    if (results.next()) {
      // Another Left Join
      val firstCommentId = results.getLong(1)
      val comments = if (results.wasNull) {
        Seq()
      } else {
        Comment(firstCommentId, results.getString(2), results.getString(3), results.getTimestamp(4).toInstant, results.getTimestamp(5).toInstant) +: makeSeq(results) {
          row => Comment(results.getLong(1), results.getString(2), results.getString(3), results.getTimestamp(4).toInstant, results.getTimestamp(5).toInstant)
        }
      }
      Some(comments)
    } else {
      None
    }
  }

  private def newCommentImpl(id: Long, comment: CommentRequest)(connection: Connection): Option[NewCommentResponse] = {
    val stmt = connection.prepareStatement(newCommentQuery, Statement.RETURN_GENERATED_KEYS)
    stmt.setString(1, comment.comment)
    stmt.setLong(2, id)
    stmt.execute()
    val results = stmt.getGeneratedKeys
    results.next()
    NewCommentResponse(results.getLong(1))
  }
  private def getTipHistoryImpl(id: Long)(connection: Connection): Option[TipHistory]  = ???

  private def getCommentImpl(id: Long)(connection: Connection): Option[Comment] = ???
  private def getCommentHistoryImpl(id: Long)(connection: Connection): Option[CommentHistory] = ???

  private def updateCommentImpl(id: Long, comment: CommentRequest, username: String)(connection: Connection): Status = {
    val checkStmt = connection.prepareStatement(updateCommentCheckQuery)
    checkStmt.setString(1, username)
    checkStmt.setLong(2, id)
    val checkResult = checkStmt.executeQuery()
    if (checkResult.next()) {
      checkResult.getLong(1)
      if (checkResult.wasNull()) {
        Forbidden // different user attempting to modify comment.
      } else {
        val stmt = connection.prepareStatement(updateCommentQuery)
        stmt.setString(1, comment.comment)
        stmt.setLong(2, id)
        val numUpdated = stmt.executeUpdate()
        assert(numUpdated == 1, s"We expected to only update one comment record in the database but $numUpdated were updated!")
        Ok
      }
    } else {
      NotFound
    }
  }



  def getTips: Future[Seq[TipNoComment]] = async(getTipsImpl)
  def newTip(tip: TipRequest, username: String): Future[NewTipResponse] = async(newTipImpl(tip, username))

  def getTip(id: Long): Future[Option[Tip]] = async(getTipImpl(id))
  def getTipNoComment(id: Long): Future[Option[TipNoComment]] = async(getTipNoCommentImpl(id))
  def updateTip(id: Long, message: TipRequest, username: String): Future[Status] = async(updateTipImpl(id, message, username))
  def getComments(id: Long): Future[Option[Seq[Comment]]] = async(getCommentsImpl(id))
  def newComment(id: Long, comment: CommentRequest): Future[Option[NewCommentResponse]] = async(newCommentImpl(id, comment))
  def getTipHistory(id: Long): Future[Option[TipHistory]]  = async(getTipHistoryImpl(id))

  def getComment(id: Long): Future[Option[Comment]] = async(getCommentImpl(id))
  def getCommentHistory(id: Long): Future[Option[CommentHistory]] = async(getCommentHistoryImpl(id))
  def updateComment(id: Long, comment: CommentRequest, username: String): Future[Status] = async(updateCommentImpl(id, comment, username))


  // Typically will spin off a transaction on a different thread pool.
  private def async[X,Y](dataCall: Connection => Y): Future[Y] = {
    Future { db.withTransaction(dataCall) }
  }

  private def makeSeq[T](results: ResultSet)(f: ResultSet => T): Seq[T] = {
    val sb =  Seq.newBuilder[T]
    while (results.next()) {
      sb += f(results)
    }
    sb.result()
  }

}
