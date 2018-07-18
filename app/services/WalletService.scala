package services

import crypto.{PrivateKey25519, PublicKey25519}
import io.iohk.iodb.ByteArrayWrapper
import models.Wallet
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Blake2b256
import scorex.crypto.signatures.PublicKey
import storage.LSMStorage

import scala.util.{Random, Try}

class WalletService {

  def loadAll: Seq[Wallet] = LSMStorage.store.get(Wallet.walletsKey).map { r =>
    r.data.sliding(32, 32).map(k => Wallet(PublicKey @@ k)).toList
  }.getOrElse(List.empty)

  def createNewWallet(so: Option[String]): Try[Wallet] = Try {
    val seed = so.getOrElse("")
    val keys: (PrivateKey25519, PublicKey25519) = PrivateKey25519.generateKeys(Blake2b256.hash(seed.getBytes()))
    val publicKey: PublicKey = keys._2.pubKeyBytes
    if (LSMStorage.store.get(Wallet.secretKey(publicKey)).isEmpty)
      LSMStorage.store.update(
        Random.nextLong(),
        Seq.empty,
        Seq(Wallet.secretKey(publicKey) -> ByteArrayWrapper(keys._1.privKeyBytes),
          Wallet.walletsKey -> ByteArrayWrapper(loadAll.flatMap(_.pubKey).toArray ++ publicKey))
      )
    Wallet(publicKey)
  }

  def restoreFromSecret(secret: String): Try[Wallet] =
    Base58.decode(secret).map { privateKey =>
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

}
