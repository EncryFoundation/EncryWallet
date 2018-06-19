package org.encryfoundation.wallet

import java.io.File
import java.lang.reflect.Constructor

import io.iohk.iodb.{ByteArrayWrapper, LSMStore}
import org.encryfoundation.wallet.account.Account
import org.encryfoundation.wallet.crypto.{PrivateKey25519, PublicKey25519}
import org.whispersystems.curve25519.OpportunisticCurve25519Provider
import scorex.crypto.hash.Blake2b256
import scorex.crypto.signatures.{PrivateKey, PublicKey}
import scorex.utils.Random

case class Wallet(store: LSMStore, pubKey: PublicKey) {

  import Wallet._

  val account: Account = Account(pubKey)

  def getSecret: PrivateKey25519 = store.get(secretKey) match {
    case Some(ByteArrayWrapper(d)) => PrivateKey25519(PrivateKey @@ d, PublicKey @@ provider.generatePublicKey(d))
    case _ => throw new Error("Secret not found")
  }
}

object Wallet {

  val secretKey: ByteArrayWrapper = ByteArrayWrapper(Blake2b256.hash("secret"))
  val initialVersionKey: ByteArrayWrapper = ByteArrayWrapper(Blake2b256.hash("initial_version"))

  private val provider: OpportunisticCurve25519Provider = {
    val constructor: Constructor[OpportunisticCurve25519Provider] = classOf[OpportunisticCurve25519Provider]
      .getDeclaredConstructors
      .head
      .asInstanceOf[Constructor[OpportunisticCurve25519Provider]]
    constructor.setAccessible(true)
    constructor.newInstance()
  }

  def initWithKey(privateKey: PrivateKey): Wallet = {
    val publicKey = PublicKey @@ provider.generatePublicKey(privateKey)
    val wallet: Wallet = readOrGenerate(publicKey)
    wallet.store.update(initialVersionKey, Seq.empty, Seq(secretKey -> ByteArrayWrapper(privateKey)))
    wallet
  }

  def initWithNewKey: Wallet = {
    val keys: (PrivateKey25519, PublicKey25519) = PrivateKey25519.generateKeys(Random.randomBytes())
    val wallet: Wallet = readOrGenerate(keys._2.pubKeyBytes)
    wallet.store.update(initialVersionKey, Seq.empty, Seq(secretKey -> ByteArrayWrapper(keys._1.privKeyBytes)))
    wallet
  }

  def readOrGenerate(pubKey: PublicKey): Wallet = {
    val dir: File = new File(s"/keys")
    dir.mkdirs()
    val store: LSMStore = new LSMStore(dir, keepVersions = 0)
    Wallet(store, pubKey)
  }
}
