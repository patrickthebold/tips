package service

import java.sql.{Connection, ResultSet, Statement, Timestamp}
import java.time.Instant
import javax.inject.Inject

import dto.DTOs._
import play.api.db.Database
import play.api.mvc.Results.Status

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Most of the work. Much Java. Very JDBC.
  */
//@Singleton
class TipsService @Inject()(db: Database) {

  val getTipsQuery =
    """
      |SELECT id, message, username, created, modified FROM tips;
    """.stripMargin

  val newTipQuery =
    """
      |INSERT INTO tips VALUES (DEFAULT, ?, ?, ?);
    """.stripMargin

  val getTipQuery =
    """
      |SELECT t.message, t.username, t.created, t.modified, c.id, c.comment, c.username, c.created, c.modified
      |FROM tips LEFT OUTER JOIN comments c
      |ON c.tip_id = t.id
      |WHERE id = ?;
    """.stripMargin

  val getTipNoCommentQuery =
    """
      |SELECT message, username, created, modified FROM tips
      |WHERE id = ?;
    """.stripMargin

  val updateTipQuery =
    """
      |UPDATE tips SET message = ?
      |WHERE id = ?;
    """.stripMargin

  val getCommentsQuery =
    """
      |SELECT c.id, c.comment, c.username, c.created, modified FROM tips t LEFT OUTER JOIN comments c
      |WHERE tip_id = ?;
    """.stripMargin

  val newCommentQuery =
    """
      |INSERT INTO comments VALUES (DEFAULT, ?, ?, ?, ?);
    """.stripMargin

  val getTipHistoryQuery =
    """
      |SELECT message, username, modified FROM tips t LEFT OUTER JOIN tips_history h
      |ON t.id = h.id
      |WHERE t.id = ?
    """.stripMargin

  val getCommentQuery =
    """
      |SELECT comment, username, created, modified FROM comments
      |WHERE id = ?;
    """.stripMargin

  val getCommentHistoryQuery =
    """
      |SELECT message, username, modified FROM comments c LEFT OUTER JOIN comments_history h
      |ON c.id = h.id
      |WHERE c.id = ?
    """.stripMargin

  val updateCommentQuery =
    """
      |UPDATE comments SET message = ?
      |WHERE id = ?;
    """.stripMargin

  // This pulls everything into memory. Will need a better approach.
  private def getTipsImpl(connection: Connection): Seq[TipNoComment] = {
    val stmt = connection.prepareStatement(getTipsQuery)
    val results = stmt.executeQuery()
    makeSeq(results) { row =>
      TipNoComment(row.getLong(1), row.getString(2), row.getString(3), row.getTimestamp(4).toInstant, row.getTimestamp(5).toInstant)
    }
  }

  private def newTipImpl(tip: TipRequest)(connection: Connection): NewTipResponse = {
    val stmt = connection.prepareStatement(newTipQuery, Statement.RETURN_GENERATED_KEYS)
    stmt.setString(1, tip.message)
    stmt.setString(2, "TODO")
    stmt.setTimestamp(3, Timestamp.from(Instant.now))
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

  private def updateTipImpl(id: Long, message: TipRequest)(connection: Connection): Status = ???
  private def getCommentsImpl(id: Long)(connection: Connection): Option[Seq[Comment]] = ???
  private def newCommentImpl(id: Long, comment: CommentRequest)(connection: Connection): Option[NewCommentResponse] = ???
  private def getTipHistoryImpl(id: Long)(connection: Connection): Option[TipHistory]  = ???

  private def getCommentImpl(id: Long)(connection: Connection): Option[Comment] = ???
  private def getCommentHistoryImpl(id: Long)(connection: Connection): Option[CommentHistory] = ???
  private def updateCommentImpl(id: Long, comment: CommentRequest)(connection: Connection): Status = ???



  def getTips(): Future[Seq[TipNoComment]] = async(getTipsImpl)
  def newTip(tip: TipRequest): Future[NewTipResponse] = async(newTipImpl(tip))

  def getTip(id: Long): Future[Option[Tip]] = async(getTipImpl(id))
  def getTipNoComment(id: Long): Future[Option[TipNoComment]] = async(getTipNoCommentImpl(id))
  def updateTip(id: Long, message: TipRequest): Future[Status] = async(updateTipImpl(id, message))
  def getComments(id: Long): Future[Option[Seq[Comment]]] = async(getCommentsImpl(id))
  def newComment(id: Long, comment: CommentRequest): Future[Option[NewCommentResponse]] = async(newCommentImpl(id, comment))
  def getTipHistory(id: Long): Future[Option[TipHistory]]  = async(getTipHistoryImpl(id))

  def getComment(id: Long): Future[Option[Comment]] = async(getCommentImpl(id))
  def getCommentHistory(id: Long): Future[Option[CommentHistory]] = async(getCommentHistoryImpl(id))
  def updateComment(id: Long, comment: CommentRequest): Future[Status] = async(updateCommentImpl(id, comment))


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
