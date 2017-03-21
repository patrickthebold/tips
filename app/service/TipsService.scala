package service

import java.sql.{Connection, Statement, Timestamp}
import java.time.Instant
import javax.inject.Inject

import dto.DTOs.{NewTip, NewTipResponse}
import play.api.db.Database

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Most of the work.
  */
//@Singleton
class TipsService @Inject()(db: Database) {

  def newTip(tip: NewTip): Future[NewTipResponse] = {
    Future {db.withConnection { connection =>
      newTip(tip, connection)
    }}
  }
  val insertTipQuery =
    """
      |INSERT INTO tips VALUES (DEFAULT, ?, ?, ?)
    """.stripMargin
  private[this] def newTip(newTip: NewTip, connection: Connection): NewTipResponse = {
    val stmt = connection.prepareStatement(insertTipQuery, Statement.RETURN_GENERATED_KEYS)
    stmt.setString(1, newTip.message)
    stmt.setString(2, "TODO")
    stmt.setTimestamp(3, Timestamp.from(Instant.now))
    stmt.executeUpdate()
    val results = stmt.getGeneratedKeys
    results.next()
    NewTipResponse(results.getLong(1))
  }



}
