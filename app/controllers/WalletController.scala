package controllers

import javax.inject.{Inject, Singleton}
import scala.util.control.NonFatal
import scala.util.Success
import scala.concurrent.ExecutionContext
import io.circe.syntax._
import play.api.mvc._
import play.api.libs.circe.Circe
import services.WalletService

@Singleton
class WalletController @Inject()(implicit ec: ExecutionContext, ws: WalletService, cc: ControllerComponents) extends AbstractController(cc) with Circe {

  def getAll: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(ws.loadAll.asJson)
  }

  def createNewWallet(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    ws.createNewWallet(request.body.asText) match {
      case Success(wallet) => Ok(wallet.asJson)
      case _ => InternalServerError
    }
  }

  def getAllWithInfo: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    ws.loadAllWithInfo().map(xs => Ok(xs.asJson)).recover { case NonFatal(_) => InternalServerError }
  }

  def restoreFromSecret(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    request.body.asFormUrlEncoded.flatMap(_.get("secretKey")).flatMap(_.headOption).flatMap(ws.restoreFromSecret(_).toOption) match {
      case Some(wallet) => Ok(wallet.asJson)
      case None => BadRequest
    }
  }

}
