package org.encryfoundation.wallet.transaction.directives

import com.google.common.primitives.{Bytes, Ints, Longs, Shorts}
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._
import org.encryfoundation.prismlang.compiler.{CompiledContract, CompiledContractSerializer}
import org.encryfoundation.wallet.transaction.EncryProposition
import org.encryfoundation.wallet.transaction.box.{AssetBox, EncryBox}
import scorex.crypto.authds
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Digest32

import scala.util.Try

case class ScriptedAssetDirective(contract: CompiledContract,
                                  amount: Long,
                                  tokenIdOpt: Option[ADKey] = None) extends Directive {

  override val typeId: Byte = ScriptedAssetDirective.TypeId

  override def boxes(digest: Digest32, idx: Int): Seq[EncryBox] =
    Seq(AssetBox(EncryProposition(contract), EncryBox.nonceFromDigest(digest ++ Ints.toByteArray(idx)), amount))

  override val cost: Long = 4

  override lazy val isValid: Boolean = amount > 0 && contract.bytes.lengthCompare(Short.MaxValue) <= 0

  lazy val isIntrinsic: Boolean = tokenIdOpt.isEmpty
}

object ScriptedAssetDirective {

  val TypeId: Byte = 3.toByte

  implicit val jsonEncoder: Encoder[ScriptedAssetDirective] = (d: ScriptedAssetDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "contract" -> Base58.encode(d.contract.bytes).asJson,
    "amount" -> d.amount.asJson,
    "tokenId" -> d.tokenIdOpt.map(id => Base58.encode(id)).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[ScriptedAssetDirective] = (c: HCursor) => for {
    contractBytes <- c.downField("contract").as[String]
    amount <- c.downField("amount").as[Long]
    tokenIdOpt <- c.downField("tokenId").as[Option[String]]
  } yield {
    val contract: CompiledContract = Base58.decode(contractBytes).flatMap(CompiledContractSerializer.parseBytes)
      .getOrElse(throw new Exception("Decoding failed"))
    ScriptedAssetDirective(contract, amount, tokenIdOpt.flatMap(id => Base58.decode(id).map(ADKey @@ _).toOption))
  }

  object Serializer {
    def toBytes(obj: ScriptedAssetDirective): Array[Byte] =
      Bytes.concat(
        Shorts.toByteArray(obj.contract.bytes.length.toShort),
        obj.contract.bytes,
        Longs.toByteArray(obj.amount),
        obj.tokenIdOpt.getOrElse(Array.empty)
      )

    def parseBytes(bytes: Array[Byte]): Try[ScriptedAssetDirective] = {
      val scriptLen: Short = Shorts.fromByteArray(bytes.take(2))
      CompiledContractSerializer.parseBytes(bytes.slice(2, scriptLen)).map { contract =>
        val amount: Long = Longs.fromByteArray(bytes.slice(scriptLen + 2, scriptLen + 2 + 8))
        val tokenIdOpt: Option[authds.ADKey] = if ((bytes.length - (scriptLen + 2 + 8)) == 32) {
          Some(ADKey @@ bytes.takeRight(32))
        } else None
        ScriptedAssetDirective(contract, amount, tokenIdOpt)
      }
    }
  }
}
