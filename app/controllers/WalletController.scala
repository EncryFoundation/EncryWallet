package controllers

import crypto.{PrivateKey25519, PublicKey25519}
import javax.inject.{Inject, Singleton}
import models.Wallet
import play.api.mvc._
import scorex.crypto.signatures.PublicKey
import storage.LSMStorage
import io.circe.syntax._
import io.iohk.iodb.ByteArrayWrapper
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Blake2b256

import scala.util.{Failure, Random, Success}

@Singleton
class WalletController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def getAll() = Action { implicit request: Request[AnyContent] =>
    Ok(loadAll.asJson.toString)
  }

  def createNewWallet() = Action { implicit request: Request[AnyContent] =>
    request.body.asText map { seed => //TODO check length otherwise we can get ArrayIndexOutOfBoundsException
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

  def restoreFromSecret() = Action { implicit request: Request[AnyContent] =>
    request.body.asText flatMap { secret =>
      Base58.decode(secret).toOption map { privateKey =>
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
      }
    } match {
        case Some(_) => Ok
        case None    => BadRequest
      }
  }


  private def loadAll  = LSMStorage.store.get(Wallet.walletsKey).map { r =>
    r.data.sliding(32, 32).map(k => Wallet(PublicKey @@ k)).toList
  }.getOrElse(List.empty)

}
