package controllers

import javax.inject.Inject

import dto.DTOs._
import play.api.mvc._
import service.TipsService
import controllers.SerializationUtil._


class TipsController @Inject()(tipsService: TipsService) extends Controller {

  def getTips = Action.async { request =>
    serialize(tipsService.getTips)
  }

  def newTip = Action.async(validateJson[TipRequest]) { request =>
    serialize(tipsService.newTip(request.body))
  }

  def getTip(id: Long, includeComments: Boolean) = Action.async { request =>
    if (includeComments) {
      serialize(tipsService.getTip(id))
    } else {
      serialize(tipsService.getTipNoComment(id))
    }
  }

  def updateTip(id: Long) = Action.async(validateJson[TipRequest]) { request =>
    serialize(tipsService.updateTip(id, request.body))
  }

  def getComments(id: Long) = Action.async { implicit request =>
    serialize(tipsService.getComments(id))
  }

  def newComment(id: Long) = Action.async(validateJson[CommentRequest]) { request =>
    serialize(tipsService.newComment(id, request.body))
  }

  def getTipHistory(id: Long) = Action.async { request =>
    serialize(tipsService.getTipHistory(id))
  }

  def getComment(id: Long) = Action.async { request =>
    serialize(tipsService.getComment(id))
  }

  def getCommentHistory(id: Long) = Action.async { request =>
    serialize(tipsService.getCommentHistory(id))
  }

  def updateComment(id: Long) = Action.async(validateJson[CommentRequest]) { request =>
    serialize(tipsService.updateComment(id, request.body))
  }


}
