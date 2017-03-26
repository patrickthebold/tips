package controllers

import javax.inject.Inject

import controllers.SerializationUtil._
import dto.DTOs._
import play.api.mvc._
import service.TipsService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.Logging




class TipsController @Inject()(tipsService: TipsService, sessionUtil: SessionUtil) extends Controller with Logging {


  def getTips = sessionUtil.Authenticated.async { implicit request =>
    tipsService.getTips map serialize[Seq[TipNoComment]]
  }

  def newTip = sessionUtil.Authenticated.async(validateJson[TipRequest]) { implicit request =>
    tipsService.newTip(request.body, request.user)  map serialize[NewTipResponse]
  }

  def getTip(id: Long, includeComments: Boolean) = sessionUtil.Authenticated.async { implicit request =>
    if (includeComments) {
      tipsService.getTip(id) map serialize[Tip]
    } else {
      tipsService.getTipNoComment(id) map serialize[TipNoComment]
    }
  }

  def updateTip(id: Long) = sessionUtil.Authenticated.async(validateJson[TipRequest]) { implicit request =>
    tipsService.updateTip(id, request.body, request.user)  map serialize
  }

  def getComments(id: Long) = sessionUtil.Authenticated.async { implicit request =>
    tipsService.getComments(id) map serialize[Seq[Comment]]
  }

  def newComment(id: Long) = sessionUtil.Authenticated.async(validateJson[CommentRequest]) { implicit request =>
    tipsService.newComment(id, request.body, request.user) map serialize[NewCommentResponse]
  }

  def getTipHistory(id: Long) = sessionUtil.Authenticated.async { implicit request =>
    tipsService.getTipHistory(id) map serialize[TipHistory]
  }

  def getComment(id: Long) = sessionUtil.Authenticated.async { implicit request =>
    tipsService.getComment(id) map serialize[StandAloneComment]
  }

  def getCommentHistory(id: Long) = sessionUtil.Authenticated.async { implicit request_ =>
    tipsService.getCommentHistory(id) map serialize[CommentHistory]
  }

  def updateComment(id: Long) = sessionUtil.Authenticated.async(validateJson[CommentRequest]) { implicit request =>
    tipsService.updateComment(id, request.body, request.user)  map serialize
  }


}
