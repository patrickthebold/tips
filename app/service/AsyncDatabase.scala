package service

import java.sql.Connection
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import play.api.db.Database

import scala.concurrent.Future

/**
  * Wraps the database to run calls in a transaction on a different threadpool.
  */
@Singleton
class AsyncDatabase @Inject()(db: Database, akkaSystem: ActorSystem) {
  private implicit val dbExecutionContext = akkaSystem.dispatchers.lookup("contexts.db")
  def async[X,Y](dataCall: Connection => Y): Future[Y] = {
    Future { db.withTransaction(dataCall) }
  }
}
