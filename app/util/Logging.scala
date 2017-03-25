package util

import org.slf4j.LoggerFactory

/**
  * mixin for logging
  */
trait Logging {

  lazy val logger = LoggerFactory.getLogger(s"tips.${getClass.getName}")

}
