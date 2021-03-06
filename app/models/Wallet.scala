package models

import java.lang.reflect.Constructor
import java.util
import io.circe.Encoder
import io.circe.syntax._
import io.iohk.iodb.ByteArrayWrapper
import org.encryfoundation.common.crypto.PublicKey25519
import org.encryfoundation.common.transaction.Pay2PubKeyAddress
import org.whispersystems.curve25519.OpportunisticCurve25519Provider
import org.encryfoundation.common.Algos
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

  val address: Pay2PubKeyAddress = Pay2PubKeyAddress(pubKey)

  override def equals(obj: Any): Boolean = obj match {
    case other: Wallet => util.Arrays.equals(pubKey, other.pubKey)
    case _ => false
  }

}

object Wallet {

  def apply(id: String): Option[Wallet] = Algos.decode(id)
    .filter(_.length == PublicKey25519.Length)
    .map(id => Wallet(PublicKey @@ id)).toOption

  implicit val jsonEncoder: Encoder[Wallet] = (w: Wallet) => Map(
    "pubKey" -> Algos.encode(w.pubKey).asJson,
    "address" -> w.address.address.asJson
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
