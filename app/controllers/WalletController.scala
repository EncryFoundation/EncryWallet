package controllers

import io.circe.syntax._
import javax.inject.{Inject, Singleton}
import play.api.libs.circe.Circe
import play.api.mvc._
import scorex.util.encode.Base58
import services.WalletService
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

@Singleton
class WalletController @Inject()(implicit ec: ExecutionContext, ws: WalletService, cc: ControllerComponents) extends AbstractController(cc) with Circe {

  def getAll: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(ws.loadAll.asJson)
  }

  def createNewWallet(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    request.body.asText match {
      case None => Ok(ws.createNewWallet(None).asJson)
      case Some(seed) if seed.isEmpty => Ok(ws.createNewWallet(None).asJson)
      case s@Some(seed) =>
        Base58.decode(seed).map(x => ws.createNewWallet(Some(x))).map(x => Ok(x.asJson)).getOrElse(BadRequest)
    }
  }

  def getAllWithInfo: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    ws.loadAllWithInfo().map(xs => Ok(xs.asJson)).recover { case NonFatal(_) => InternalServerError }
  }

  def restoreFromSecret(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>

    val secretKey: Option[Array[Byte]] = request.body.asFormUrlEncoded
      .flatMap(_.get("secretKey"))
      .flatMap(_.headOption)
      .flatMap(x => Base58.decode(x).toOption)

    secretKey match {
      case Some(s) => Ok(ws.restoreFromSecret(s).asJson)
      case None => BadRequest
    }
  }

}
