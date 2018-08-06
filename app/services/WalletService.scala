package services

import crypto.{PrivateKey25519, PublicKey25519}
import io.iohk.iodb.ByteArrayWrapper
import javax.inject.Inject
import models.{Wallet, WalletInfo}
import scorex.crypto.hash.Blake2b256
import scorex.crypto.signatures.PublicKey
import storage.LSMStorage
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class WalletService @Inject()(implicit ec: ExecutionContext, lsmStorage: LSMStorage, es: ExplorerService) {

  def loadAll: Seq[Wallet] = lsmStorage.store.get(Wallet.walletsKey).map { r =>
    r.data.sliding(PublicKey25519.Length, PublicKey25519.Length).map(k => Wallet(PublicKey @@ k)).toList
  }.getOrElse(List.empty)

  def createNewWallet(seedOpt: Option[Array[Byte]]): Wallet = {
    val seed: Array[Byte] = seedOpt.getOrElse(scorex.utils.Random.randomBytes(32))
    val keys: (PrivateKey25519, PublicKey25519) = PrivateKey25519.generateKeys(Blake2b256.hash(seed))
    val publicKey: PublicKey = keys._2.pubKeyBytes
    if (lsmStorage.store.get(Wallet.secretKey(publicKey)).isEmpty)
      lsmStorage.store.update(
        Random.nextLong(),
        Seq.empty,
        Seq(Wallet.secretKey(publicKey) -> ByteArrayWrapper(keys._1.privKeyBytes),
          Wallet.walletsKey -> ByteArrayWrapper(loadAll.flatMap(_.pubKey).toArray ++ publicKey))
      )
    Wallet(publicKey)
  }

  def restoreFromSecret(secret: Array[Byte]): Wallet = {
    val publicKey: PublicKey = PublicKey @@ Wallet.provider.generatePublicKey(secret)
    if (lsmStorage.store.get(Wallet.secretKey(publicKey)).isEmpty)
      lsmStorage.store.update(
        Random.nextLong(),
        Seq.empty,
        Seq(
          Wallet.secretKey(publicKey) -> ByteArrayWrapper(secret),
          Wallet.walletsKey -> ByteArrayWrapper(loadAll.flatMap(_.pubKey).toArray ++ publicKey)
        )
      )
    Wallet(publicKey)
  }

  def loadWalletInfo(w: Wallet): Future[WalletInfo] = es.requestUtxos(w.address.address).map(outputs => WalletInfo(w, outputs.map(_.monetaryValue).sum))

  def loadAllWithInfo(): Future[Seq[WalletInfo]] = Future.sequence(loadAll.map(loadWalletInfo))

}

