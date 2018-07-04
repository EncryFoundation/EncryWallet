package org.encryfoundation.wallet.actions

import io.iohk.iodb.ByteArrayWrapper
import org.encryfoundation.wallet.crypto.{PrivateKey25519, PublicKey25519}
import org.encryfoundation.wallet.{Wallet, WalletApp}
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Blake2b256
import scorex.crypto.signatures.PublicKey

import scala.util.Random

object WalletActions {

  import Wallet._

  def createNewWallet(seed: String): Wallet = {
    val keys: (PrivateKey25519, PublicKey25519) = PrivateKey25519.generateKeys(Blake2b256.hash(seed.getBytes()))
    val publicKey: PublicKey = keys._2.pubKeyBytes
    if (WalletApp.store.get(secretKey(publicKey)).isEmpty)
      WalletApp.store.update(
        Random.nextLong(),
        Seq.empty,
        Seq(
          secretKey(publicKey) -> ByteArrayWrapper(keys._1.privKeyBytes),
          walletsKey -> ByteArrayWrapper((loadAll.map(_.pubKey) :+ publicKey).foldLeft(Array.empty[Byte])(_ ++ _))
        )
      )
    Wallet(publicKey)
  }

  def restoreFromSecret(secret: String): Option[Wallet] = Base58.decode(secret).map { privateKey =>
    val publicKey: PublicKey = PublicKey @@ provider.generatePublicKey(privateKey)
    if (WalletApp.store.get(secretKey(publicKey)).isEmpty)
      WalletApp.store.update(
        Random.nextLong(),
        Seq.empty,
        Seq(
          secretKey(publicKey) -> ByteArrayWrapper(privateKey),
          walletsKey -> ByteArrayWrapper((loadAll.map(_.pubKey) :+ publicKey).foldLeft(Array.empty[Byte])(_ ++ _))
        )
      )
    Wallet(publicKey)
  }.toOption

  def loadAll: List[Wallet] = WalletApp.store.get(walletsKey).map { r =>
    r.data.sliding(32, 32).map(k => Wallet(PublicKey @@ k)).toList
  }.getOrElse(List.empty)
}
