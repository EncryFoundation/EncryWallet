package models.box

import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._
import models.EncryProposition
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base16

/** Represents monetary asset of some type locked with some `proposition`.
  * `tokenIdOpt = None` if the asset is of intrinsic type. */
case class AssetBox(proposition: EncryProposition,
                    nonce: Long,
                    value: Long,
                    tokenIdOpt: Option[ADKey] = None) extends EncryBox {

  override val typeId: Byte = AssetBox.TypeId

  lazy val isIntrinsic: Boolean = tokenIdOpt.isEmpty
}

object AssetBox {

  val TypeId: Byte = 1.toByte

  implicit val jsonEncoder: Encoder[AssetBox] = (bx: AssetBox) => Map(
    "typeId" -> TypeId.asJson,
    "id" -> Base16.encode(bx.id).asJson,
    "proposition" -> bx.proposition.asJson,
    "nonce" -> bx.nonce.asJson,
    "value" -> bx.value.asJson,
    "tokenId" -> bx.tokenIdOpt.map(id => Base16.encode(id)).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[AssetBox] = (c: HCursor) => for {
    nonce <- c.downField("nonce").as[Long]
    proposition <- c.downField("proposition").as[EncryProposition]
    value <- c.downField("value").as[Long]
    tokenIdOpt <-c.downField("tokenId").as[Option[String]]
  } yield AssetBox(proposition, nonce, value, tokenIdOpt.flatMap(id => Base16.decode(id).map(ADKey @@ _).toOption))
}
