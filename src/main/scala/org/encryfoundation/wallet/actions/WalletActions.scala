package org.encryfoundation.wallet.actions

import io.iohk.iodb.ByteArrayWrapper
import org.encryfoundation.wallet.crypto.{PrivateKey25519, PublicKey25519}
import org.encryfoundation.wallet.{Wallet, WalletApp, WalletInfo}
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Blake2b256
import scorex.crypto.signatures.PublicKey

import scala.concurrent.Future
import scala.util.Random

object WalletActions {

  import NetworkActions._
  import Wallet._

  import WalletApp.executionContext

  def createNewWallet(seed: String): Wallet = {
    val keys: (PrivateKey25519, PublicKey25519) = PrivateKey25519.generateKeys(Blake2b256.hash(seed.getBytes()))
    val publicKey: PublicKey = keys._2.pubKeyBytes
    if (WalletApp.store.get(secretKey(publicKey)).isEmpty)
      WalletApp.store.update(
        Random.nextLong(),
        Seq.empty,
        Seq(
          secretKey(publicKey) -> ByteArrayWrapper(keys._1.privKeyBytes),
          walletsKey -> ByteArrayWrapper(loadAll.flatMap(_.pubKey).toArray ++ publicKey)
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
          walletsKey -> ByteArrayWrapper(loadAll.flatMap(_.pubKey).toArray ++ publicKey)
        )
      )
    Wallet(publicKey)
  }.toOption

  def loadAll: List[Wallet] = WalletApp.store.get(walletsKey).map { r =>
    r.data.sliding(32, 32).map(k => Wallet(PublicKey @@ k)).toList
  }.getOrElse(List.empty)

  def loadAllWithExtendedInfo: Future[List[WalletInfo]] = WalletApp.store.get(walletsKey).map { r =>
    val wallets: List[Wallet] = r.data.sliding(32, 32).map(k => Wallet(PublicKey @@ k)).toList
    Future.sequence(wallets.map { w =>
      requestUtxos(w.account.address).map(bxs => WalletInfo(w, bxs.map(_.value).sum))
    })
  }.getOrElse(Future.failed(new Exception("Storage is not initialised")))

  def fromId(id: String): Option[Wallet] = Base58.decode(id).map(id => Wallet(PublicKey @@ id)).toOption
}
