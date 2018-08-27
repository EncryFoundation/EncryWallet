package models.directives

import scala.util.Try
import com.google.common.primitives.{Bytes, Ints, Longs}
import models.directives.Directive.DTypeId
import org.encryfoundation.common.serialization.Serializer
import models.box.Box.Amount
import models.box.{AssetBox, EncryBaseBox, EncryProposition}
import org.encryfoundation.common.{Algos, Constants}
import org.encryfoundation.common.utils.Utils
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.utils.TaggedTypes.ADKey
import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import scorex.crypto.hash.Digest32

case class ScriptedAssetDirective(contractHash: ContractHash,
                                  amount: Amount,
                                  tokenIdOpt: Option[ADKey] = None) extends Directive {

  override type M = ScriptedAssetDirective

  override val typeId: DTypeId = ScriptedAssetDirective.TypeId

  override def boxes(digest: Digest32, idx: Int): Seq[EncryBaseBox] =
    Seq(AssetBox(EncryProposition(contractHash), Utils.nonceFromDigest(digest ++ Ints.toByteArray(idx)), amount))

  override lazy val isValid: Boolean = amount > 0

  override def serializer: Serializer[M] = ScriptedAssetDirectiveSerializer

  lazy val isIntrinsic: Boolean = tokenIdOpt.isEmpty
}

object ScriptedAssetDirective {

  val TypeId: DTypeId = 3.toByte

  implicit val jsonEncoder: Encoder[ScriptedAssetDirective] = (d: ScriptedAssetDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "contractHash" -> Algos.encode(d.contractHash).asJson,
    "amount" -> d.amount.asJson,
    "tokenId" -> d.tokenIdOpt.map(id => Algos.encode(id)).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[ScriptedAssetDirective] = (c: HCursor) => for {
    contractHash <- c.downField("contractHash").as[ContractHash](Decoder.decodeString.emapTry(Algos.decode))
    amount <- c.downField("amount").as[Long]
    tokenIdOpt <- c.downField("tokenId").as[Option[ADKey]](Decoder.decodeOption(Decoder.decodeString.emapTry(Algos.decode).map(ADKey @@ _)))
  } yield ScriptedAssetDirective(contractHash, amount, tokenIdOpt)
}

object ScriptedAssetDirectiveSerializer extends Serializer[ScriptedAssetDirective] {

  override def toBytes(obj: ScriptedAssetDirective): Array[Byte] =
    Bytes.concat(
      obj.contractHash,
      Longs.toByteArray(obj.amount),
      obj.tokenIdOpt.getOrElse(Array.empty)
    )

  override def parseBytes(bytes: Array[Byte]): Try[ScriptedAssetDirective] = Try {
    val contractHash: ContractHash = bytes.take(Constants.DigestLength)
    val amount: Amount = Longs.fromByteArray(bytes.slice(Constants.DigestLength, Constants.DigestLength + 8))
    val tokenIdOpt: Option[ADKey] = if ((bytes.length - (Constants.DigestLength + 8)) == Constants.ModifierIdSize) {
      Some(ADKey @@ bytes.takeRight(Constants.ModifierIdSize))
    } else None
    ScriptedAssetDirective(contractHash, amount, tokenIdOpt)
  }
}
