package controllers

import javax.inject.{Inject, Singleton}
import models.Wallet
import play.api.mvc._
import scorex.crypto.signatures.PublicKey
import storage.LSMStorage
import io.circe.syntax._

@Singleton
class WalletController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def getAll() = Action { implicit request: Request[AnyContent] =>

    val ws = LSMStorage.store.get(Wallet.walletsKey).map { r =>
      r.data.sliding(32, 32).map(k => Wallet(PublicKey @@ k)).toList
    }.getOrElse(List.empty)

    Ok(ws.asJson.toString)

  }

}
