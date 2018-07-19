package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import io.circe.syntax._
import play.api.libs.circe.Circe
import services.WalletService
import scala.util.Success

@Singleton
class WalletController @Inject()(ws: WalletService, cc: ControllerComponents) extends AbstractController(cc) with Circe {

  def getAll: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(ws.loadAll.asJson)
  }

  def createNewWallet(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    ws.createNewWallet(request.body.asText) match {
      case Success(wallet) => Ok(wallet.asJson)
      case _ => InternalServerError
    }
  }

  def restoreFromSecret(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    request.queryString.get("secretKey").flatMap(_.headOption).flatMap(ws.restoreFromSecret(_).toOption) match {
      case Some(wallet) => Ok(wallet.asJson)
      case None => BadRequest
    }
  }

}
