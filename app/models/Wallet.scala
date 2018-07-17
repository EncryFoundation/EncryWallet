package models

import java.lang.reflect.Constructor

import io.circe.Encoder
import io.circe.syntax._
import io.iohk.iodb.ByteArrayWrapper
import org.whispersystems.curve25519.OpportunisticCurve25519Provider
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Blake2b256
import scorex.crypto.signatures.PublicKey

case class WalletInfo(wallet: Wallet, balance: Long)

object WalletInfo {

  implicit val jsonEncoder: Encoder[WalletInfo] = (wi: WalletInfo) => Map(
    "wallet" -> wi.wallet.asJson,
    "balance" -> wi.balance.asJson
  ).asJson

}

case class Wallet(pubKey: PublicKey) {

  val account: Account = Account(pubKey)

}

object Wallet {

  def fromId(id: String): Option[Wallet] = Base58.decode(id).map(id => Wallet(PublicKey @@ id)).toOption

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
