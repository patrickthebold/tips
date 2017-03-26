package controllers

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Results._

/**
  * Help make api self discoverable. Todo: need more detail about the names of the keys for this
  * to really be self discoverable.
  */
class IndexController {

  def index = Action { implicit request =>
    val api = Json.obj(
      "login_ref" -> controllers.routes.UserController.login().absoluteURL(),
      "create_user_ref" -> controllers.routes.UserController.newUser().absoluteURL(),
      "logout_ref" -> controllers.routes.UserController.logout().absoluteURL(),
      "get_tips_ref" -> controllers.routes.TipsController.getTips().absoluteURL(),
      "post_new_tip_ref" -> controllers.routes.TipsController.newTip().absoluteURL()
    )
    Ok(api)
  }
}
