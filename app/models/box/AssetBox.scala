package models.box

import scala.util.Try
import com.google.common.primitives.{Bytes, Longs, Shorts}
import models.box.Box.Amount
import models.box.EncryBox.BxTypeId
import models.box.TokenIssuingBox.TokenId
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._
import org.encryfoundation.common.{Algos, Constants}
import org.encryfoundation.common.serialization.Serializer

/** Represents monetary asset of some type locked with some `proposition`.
  * `tokenIdOpt = None` if the asset is of intrinsic type. */
case class AssetBox(override val proposition: EncryProposition,
                    override val nonce: Long,
                    override val amount: Amount,
                    tokenIdOpt: Option[TokenId] = None)
  extends EncryBox[EncryProposition] with MonetaryBox {

  override type M = AssetBox

  override val typeId: BxTypeId = AssetBox.TypeId

  override def serializer: Serializer[M] = AssetBoxSerializer

  lazy val isIntrinsic: Boolean = tokenIdOpt.isEmpty

}

object AssetBox {

  val TypeId: BxTypeId = 1.toByte

  implicit val jsonEncoder: Encoder[AssetBox] = (bx: AssetBox) => Map(
    "type" -> TypeId.asJson,
    "id" -> Algos.encode(bx.id).asJson,
    "proposition" -> bx.proposition.asJson,
    "nonce" -> bx.nonce.asJson,
    "value" -> bx.amount.asJson,
    "tokenId" -> bx.tokenIdOpt.map(id => Algos.encode(id)).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[AssetBox] = (c: HCursor) => for {
    nonce <- c.downField("nonce").as[Long]
    proposition <- c.downField("proposition").as[EncryProposition]
    value <- c.downField("value").as[Long]
    tokenIdOpt <- c.downField("tokenId").as[Option[TokenId]](Decoder.decodeOption(Decoder.decodeString.emapTry(Algos.decode)))
  } yield AssetBox(proposition, nonce, value, tokenIdOpt)
}

object AssetBoxSerializer extends Serializer[AssetBox] {

  override def toBytes(obj: AssetBox): Array[Byte] = {
    val propBytes = EncryPropositionSerializer.toBytes(obj.proposition)
    Bytes.concat(
      Shorts.toByteArray(propBytes.length.toShort),
      propBytes,
      Longs.toByteArray(obj.nonce),
      Longs.toByteArray(obj.amount),
      obj.tokenIdOpt.getOrElse(Array.empty)
    )
  }

  override def parseBytes(bytes: Array[Byte]): Try[AssetBox] = Try {
    val propositionLen: Short = Shorts.fromByteArray(bytes.take(2))
    val iBytes: Array[BxTypeId] = bytes.drop(2)
    val proposition: EncryProposition = EncryPropositionSerializer.parseBytes(iBytes.take(propositionLen)).get
    val nonce: Amount = Longs.fromByteArray(iBytes.slice(propositionLen, propositionLen + 8))
    val amount: Amount = Longs.fromByteArray(iBytes.slice(propositionLen + 8, propositionLen + 8 + 8))
    val tokenIdOpt: Option[TokenId] = if ((iBytes.length - (propositionLen + 8 + 8)) == Constants.ModifierIdSize) {
      Some(iBytes.takeRight(Constants.ModifierIdSize))
    } else None
    AssetBox(proposition, nonce, amount, tokenIdOpt)
  }
}
