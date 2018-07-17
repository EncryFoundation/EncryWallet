package controllers

import crypto.{PrivateKey25519, PublicKey25519}
import javax.inject.{Inject, Singleton}
import models.Wallet
import play.api.mvc._
import scorex.crypto.signatures.PublicKey
import storage.LSMStorage
import io.circe.syntax._
import io.iohk.iodb.ByteArrayWrapper
import play.api.libs.circe.Circe
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Blake2b256
import scala.util.{Random, Try}

@Singleton
class WalletController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Circe {

  def getAll(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(loadAll.asJson)
  }

  def createNewWallet(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    request.body.asText map { seed =>
      val keys: (PrivateKey25519, PublicKey25519) = PrivateKey25519.generateKeys(Blake2b256.hash(seed.getBytes()))
      val publicKey: PublicKey = keys._2.pubKeyBytes
      if (LSMStorage.store.get(Wallet.secretKey(publicKey)).isEmpty)
        LSMStorage.store.update(
          Random.nextLong(),
          Seq.empty,
          Seq(
            Wallet.secretKey(publicKey) -> ByteArrayWrapper(keys._1.privKeyBytes),
            Wallet.walletsKey -> ByteArrayWrapper(loadAll.flatMap(_.pubKey).toArray ++ publicKey)
          )
        )
      Wallet(publicKey)
    } match {
      case Some(_) => Ok
      case None    => BadRequest
    }
  }

  def restoreFromSecret(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    request.body.asText flatMap { secret =>
      Base58.decode(secret) flatMap { privateKey => Try {
        val publicKey: PublicKey = PublicKey @@ Wallet.provider.generatePublicKey(privateKey)
        if (LSMStorage.store.get(Wallet.secretKey(publicKey)).isEmpty)
          LSMStorage.store.update(
            Random.nextLong(),
            Seq.empty,
            Seq(
              Wallet.secretKey(publicKey) -> ByteArrayWrapper(privateKey),
              Wallet.walletsKey -> ByteArrayWrapper(loadAll.flatMap(_.pubKey).toArray ++ publicKey)
            )
          )
        Wallet(publicKey)
      } } toOption
    } match {
        case Some(_) => Ok
        case None    => BadRequest
      }
  }


  private def loadAll: Seq[Wallet] = LSMStorage.store.get(Wallet.walletsKey).map { r =>
    r.data.sliding(32, 32).map(k => Wallet(PublicKey @@ k)).toList
  }.getOrElse(List.empty)

}
