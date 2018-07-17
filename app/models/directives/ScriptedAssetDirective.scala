package models.directives

import com.google.common.primitives.{Bytes, Ints, Longs}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import models.EncryProposition
import models.box.{AssetBox, EncryBox}
import scorex.crypto.authds
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base16
import scorex.crypto.hash.Digest32

import scala.util.Try

case class ScriptedAssetDirective(contractHash: ContractHash,
                                  amount: Long,
                                  tokenIdOpt: Option[ADKey] = None) extends Directive {

  override val typeId: Byte = ScriptedAssetDirective.TypeId

  override def boxes(digest: Digest32, idx: Int): Seq[EncryBox] =
    Seq(AssetBox(EncryProposition(contractHash), EncryBox.nonceFromDigest(digest ++ Ints.toByteArray(idx)), amount))

  override lazy val isValid: Boolean = amount > 0

  lazy val isIntrinsic: Boolean = tokenIdOpt.isEmpty
}

object ScriptedAssetDirective {

  val TypeId: Byte = 3.toByte

  implicit val jsonEncoder: Encoder[ScriptedAssetDirective] = (d: ScriptedAssetDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "contractHash" -> Base16.encode(d.contractHash).asJson,
    "amount" -> d.amount.asJson,
    "tokenId" -> d.tokenIdOpt.map(id => Base16.encode(id)).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[ScriptedAssetDirective] = (c: HCursor) => for {
    contractHash <- c.downField("contractHash").as[String]
    amount <- c.downField("amount").as[Long]
    tokenIdOpt <- c.downField("tokenId").as[Option[String]]
  } yield Base16.decode(contractHash)
    .map(ch => ScriptedAssetDirective(ch, amount, tokenIdOpt.flatMap(id => Base16.decode(id).map(ADKey @@ _).toOption)))
    .getOrElse(throw new Exception("Decoding failed"))

  object Serializer {
    def toBytes(obj: ScriptedAssetDirective): Array[Byte] =
      Bytes.concat(
        obj.contractHash,
        Longs.toByteArray(obj.amount),
        obj.tokenIdOpt.getOrElse(Array.empty)
      )
    def parseBytes(bytes: Array[Byte]): Try[ScriptedAssetDirective] = Try {
      val contractHash: ContractHash = bytes.take(32)
      val amount: Long = Longs.fromByteArray(bytes.slice(32, 32 + 8))
      val tokenIdOpt: Option[authds.ADKey] = if ((bytes.length - (32 + 8)) == 32) {
        Some(ADKey @@ bytes.takeRight(32))
      } else None
      ScriptedAssetDirective(contractHash, amount, tokenIdOpt)
    }
  }
}
