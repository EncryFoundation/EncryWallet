package models.directives

import com.google.common.primitives.{Bytes, Ints, Longs}
import models.directives.Directive.DTypeId
import org.encryfoundation.common.serialization.Serializer
import models.box.Box.Amount
import models.box.{TokenIssuingBox, EncryBaseBox, EncryProposition}
import org.encryfoundation.common.{Algos, Constants}
import org.encryfoundation.common.utils.Utils
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import scorex.crypto.hash.Digest32
import scala.util.Try

case class AssetIssuingDirective(contractHash: ContractHash, amount: Amount) extends Directive {

  override type M = AssetIssuingDirective
  override val typeId: DTypeId = AssetIssuingDirective.TypeId
  override lazy val isValid: Boolean = amount > 0

  override def boxes(digest: Digest32, idx: Int): Seq[EncryBaseBox] =
    Seq(TokenIssuingBox(
      EncryProposition(contractHash),
      Utils.nonceFromDigest(digest ++ Ints.toByteArray(idx)),
      amount,
      Algos.hash(Ints.toByteArray(idx) ++ digest)
    ))

  override def serializer: Serializer[M] = AssetIssuingDirectiveSerializer
}

object AssetIssuingDirective {

  val TypeId: DTypeId = 2.toByte

  implicit val jsonEncoder: Encoder[AssetIssuingDirective] = (d: AssetIssuingDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "contractHash" -> Algos.encode(d.contractHash).asJson,
    "amount" -> d.amount.asJson
  ).asJson

  implicit val jsonDecoder: Decoder[AssetIssuingDirective] = (c: HCursor) => {
    for {
      contractHash <- c.downField("contractHash").as[ContractHash](Decoder.decodeString.emapTry(Algos.decode))
      amount <- c.downField("amount").as[Long]
    } yield AssetIssuingDirective(contractHash, amount)
  }
}

object AssetIssuingDirectiveSerializer extends Serializer[AssetIssuingDirective] {

  override def toBytes(obj: AssetIssuingDirective): Array[Byte] =
    Bytes.concat(
      obj.contractHash,
      Longs.toByteArray(obj.amount)
    )

  override def parseBytes(bytes: Array[Byte]): Try[AssetIssuingDirective] = Try {
    val contractHash: ContractHash = bytes.take(Constants.DigestLength)
    val amount: Amount = Longs.fromByteArray(bytes.slice(Constants.DigestLength, Constants.DigestLength + 8))
    AssetIssuingDirective(contractHash, amount)
  }
}
