package models.directives

import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import com.google.common.primitives.{Bytes, Ints}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import models.box.{DataBox, EncryProposition}
import models.directives.Directive.DTypeId
import org.encryfoundation.common.serialization.Serializer
import org.encryfoundation.common.utils.Utils
import org.encryfoundation.common.{Algos, Constants}
import scorex.crypto.hash.Digest32
import scala.util.Try

case class DataDirective(contractHash: ContractHash, data: Array[Byte]) extends Directive {

  override type M = DataDirective

  override val typeId: DTypeId = DataDirective.TypeId

  override def boxes(digest: Digest32, idx: Int): Seq[DataBox] =
    Seq(DataBox(EncryProposition(contractHash), Utils.nonceFromDigest(digest ++ Ints.toByteArray(idx)), data))

  val MaxDataLength: Int = 1000

  override lazy val isValid: Boolean = data.length <= MaxDataLength

  override def serializer: Serializer[M] = DataDirectiveSerializer

}

object DataDirective {

  val TypeId: DTypeId = 5.toByte

  implicit val jsonEncoder: Encoder[DataDirective] = (d: DataDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "contractHash" -> Algos.encode(d.contractHash).asJson,
    "data" -> Algos.encode(d.data).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[DataDirective] = (c: HCursor) => {
    for {
      contractHash <- c.downField("contractHash").as[String]
      dataEnc <- c.downField("data").as[String]
    } yield Algos.decode(contractHash)
      .flatMap(ch => Algos.decode(dataEnc).map(data =>  DataDirective(ch, data)))
      .getOrElse(throw new Exception("Decoding failed"))
  }
}

object DataDirectiveSerializer extends Serializer[DataDirective] {

  override def toBytes(obj: DataDirective): Array[Byte] =
    Bytes.concat(
      obj.contractHash,
      Ints.toByteArray(obj.data.length),
      obj.data
    )

  override def parseBytes(bytes: Array[Byte]): Try[DataDirective] = Try {
    val contractHash: ContractHash = bytes.take(Constants.DigestLength)
    val dataLen: Int = Ints.fromByteArray(bytes.slice(Constants.DigestLength, Constants.DigestLength + 4))
    val data: Array[DTypeId] = bytes.slice(Constants.DigestLength + 4, Constants.DigestLength + 4 + dataLen)
    DataDirective(contractHash, data)
  }
}