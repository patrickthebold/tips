package controllers

import javax.inject.Inject

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import controllers.SerializationUtil._
import dto.DTOs.User
import play.api.mvc.Results._
import play.api.mvc._
import service.UserService

/**
  * Authentication and user creation.
  */
class UserController @Inject()(userService: UserService, sessionUtil: SessionUtil) {



  def login = Action.async(validateJson[User]) { implicit request =>
    userService.login(request.body) map response
  }

  def newUser = Action.async(validateJson[User]) { implicit request =>
    userService.newUser(request.body) map response
  }

  def logout = Action { _ => Ok.withNewSession }

  private def response(valid: Boolean)(implicit request: Request[User]): Result = {
    if (valid) {
      Ok.withSession(("username", request.body.username), sessionUtil.nextExpiration)
    } else {
      BadRequest
    }
  }

}
