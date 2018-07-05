package org.encryfoundation.wallet

import java.lang.reflect.Constructor

import io.circe.Encoder
import io.circe.syntax._
import io.iohk.iodb.ByteArrayWrapper
import org.encryfoundation.wallet.account.Account
import org.encryfoundation.wallet.crypto.PrivateKey25519
import org.whispersystems.curve25519.OpportunisticCurve25519Provider
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Blake2b256
import scorex.crypto.signatures.{PrivateKey, PublicKey}

case class WalletInfo(wallet: Wallet, balance: Long)

object WalletInfo {
  implicit val jsonEncoder: Encoder[WalletInfo] = (wi: WalletInfo) => Map(
    "wallet" -> wi.wallet.asJson,
    "balance" -> wi.balance.asJson
  ).asJson
}

case class Wallet(pubKey: PublicKey) {

  import Wallet._

  val account: Account = Account(pubKey)

  def getSecret: PrivateKey25519 = WalletApp.store.get(secretKey(pubKey)) match {
    case Some(ByteArrayWrapper(d)) => PrivateKey25519(PrivateKey @@ d, PublicKey @@ provider.generatePublicKey(d))
    case _ => throw new Exception("Secret not found")
  }
}

object Wallet {

  implicit val jsonEncoder: Encoder[Wallet] = (w: Wallet) => Map(
    "pubKey" -> Base58.encode(w.pubKey).asJson,
    "address" -> w.account.address.asJson
  ).asJson

  val walletsKey: ByteArrayWrapper = ByteArrayWrapper(Blake2b256.hash("wallets"))

  def secretKey(pk: PublicKey): ByteArrayWrapper = ByteArrayWrapper(Blake2b256.hash(pk))

  val provider: OpportunisticCurve25519Provider = {
    val constructor: Constructor[OpportunisticCurve25519Provider] = classOf[OpportunisticCurve25519Provider]
      .getDeclaredConstructors
      .head
      .asInstanceOf[Constructor[OpportunisticCurve25519Provider]]
    constructor.setAccessible(true)
    constructor.newInstance()
  }
}
