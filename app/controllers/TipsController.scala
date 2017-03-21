package controllers

import javax.inject.Inject

import dto.DTOs._
import play.api.libs.json.{JsError, Json, Reads}
import play.api.mvc._
import service.TipsService
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
class TipsController @Inject()(tipsService: TipsService) extends Controller {

  // This helper parses and validates JSON using the implicit `placeReads`
  // above, returning errors if the parsed json fails validation.
  def validateJson[A : Reads] = BodyParsers.parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )


  def newTip = Action.async(validateJson[NewTip]) { implicit request =>
    tipsService.newTip(request.body) map {resp => Ok(Json.toJson(resp))}
  }


}
