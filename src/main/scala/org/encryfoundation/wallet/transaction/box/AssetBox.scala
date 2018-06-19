package org.encryfoundation.wallet.transaction.box

import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._
import org.encryfoundation.wallet.transaction.EncryProposition
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base58

/** Represents monetary asset of some type locked with some `proposition`.
  * `tokenIdOpt = None` if the asset is of intrinsic type. */
case class AssetBox(proposition: EncryProposition,
                    nonce: Long,
                    amount: Long,
                    tokenIdOpt: Option[ADKey] = None) extends EncryBox {

  override val typeId: Byte = AssetBox.TypeId

  lazy val isIntrinsic: Boolean = tokenIdOpt.isEmpty
}

object AssetBox {

  val TypeId: Byte = 1.toByte

  implicit val jsonEncoder: Encoder[AssetBox] = (bx: AssetBox) => Map(
    "type" -> TypeId.asJson,
    "id" -> Base58.encode(bx.id).asJson,
    "proposition" -> bx.proposition.asJson,
    "nonce" -> bx.nonce.asJson,
    "value" -> bx.amount.asJson,
    "tokenId" -> bx.tokenIdOpt.map(id => Base58.encode(id)).asJson
  ).asJson

  implicit val decodeEncryProposition: Decoder[EncryProposition] = (c: HCursor) => for {
    address <- c.downField("script").as[String]
  } yield EncryProposition.accountLock(address)


  implicit val decodeAssetBox: Decoder[AssetBox] = (c: HCursor) => for {
    nonce <- c.downField("nonce").as[Long]
    id <- c.downField("id").as[String]
    proposition <- c.downField("proposition").as[EncryProposition]
    amount <- c.downField("value").as[Long]
  } yield new AssetBox(proposition, nonce, amount)

}
