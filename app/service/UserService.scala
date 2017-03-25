package service

import java.sql.Connection
import javax.inject.{Inject, Singleton}

import dto.DTOs.User
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.Future

/**
  * Interact with database
  * Warning: Lots of mutable state working with Java APIs!
  */
@Singleton
class UserService @Inject()(db: AsyncDatabase) {

  private val newUserQuery =
    """
      |INSERT INTO users VALUES (?, ?)
      |ON CONFLICT DO NOTHING;
    """.stripMargin

  private val loginQuery =
    """
      |SELECT pw_hash from users
      |WHERE username = ?;
    """.stripMargin

  private def newUserImpl(user: User)(connection: Connection): Boolean = {
    val stmt = connection.prepareStatement(newUserQuery)
    stmt.setString(1, user.username)
    stmt.setString(2,  BCrypt.hashpw(user.password, BCrypt.gensalt()))
    stmt.executeUpdate() == 1
  }

  private def loginImpl(user: User)(connection: Connection): Boolean = {
    val stmt = connection.prepareStatement(loginQuery)
    stmt.setString(1, user.username)
    val results = stmt.executeQuery()
    results.next() && BCrypt.checkpw(user.password, results.getString(1))
  }

  def newUser(user: User): Future[Boolean] = db.async(newUserImpl(user))
  def login(user: User): Future[Boolean] = db.async(loginImpl(user))

}
