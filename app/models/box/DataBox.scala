package models.box

import scala.util.Try
import com.google.common.primitives.{Bytes, Longs, Shorts}
import org.encryfoundation.common.serialization.Serializer
import models.box.EncryBox.BxTypeId
import io.circe.Encoder
import io.circe.syntax._
import org.encryfoundation.common.Algos
import org.encryfoundation.prismlang.core.Types
import org.encryfoundation.prismlang.core.wrapped.{PObject, PValue}

/** Stores arbitrary data in EncryTL binary format. */
case class DataBox(override val proposition: EncryProposition,
                   override val nonce: Long,
                   data: Array[Byte])
  extends EncryBox[EncryProposition] {

  override type M = DataBox

  override val typeId: BxTypeId = DataBox.TypeId

  override def serializer: Serializer[M] = DataBoxSerializer

  override val tpe: Types.Product = Types.DataBox

  override def asVal: PValue = PValue(asPrism, Types.DataBox)

  override def asPrism: PObject =
    PObject(baseFields ++ Map(
      "data" -> PValue(data, Types.PCollection.ofByte)
    ), tpe)
}

object DataBox {

  val TypeId: BxTypeId = 4.toByte

  implicit val jsonEncoder: Encoder[DataBox] = (bx: DataBox) => Map(
    "type" -> TypeId.asJson,
    "id" -> Algos.encode(bx.id).asJson,
    "proposition" -> bx.proposition.asJson,
    "nonce" -> bx.nonce.asJson,
    "data" -> Algos.encode(bx.data).asJson,
  ).asJson
}

object DataBoxSerializer extends Serializer[DataBox] {

  override def toBytes(obj: DataBox): Array[Byte] = {
    val propBytes: Array[BxTypeId] = EncryPropositionSerializer.toBytes(obj.proposition)
    Bytes.concat(
      Shorts.toByteArray(propBytes.length.toShort),
      propBytes,
      Longs.toByteArray(obj.nonce),
      Shorts.toByteArray(obj.data.length.toShort),
      obj.data
    )
  }

  override def parseBytes(bytes: Array[Byte]): Try[DataBox] = Try {
    val propositionLen: Short = Shorts.fromByteArray(bytes.take(2))
    val iBytes: Array[BxTypeId] = bytes.drop(2)
    val proposition: EncryProposition = EncryPropositionSerializer.parseBytes(iBytes.take(propositionLen)).get
    val nonce: Long = Longs.fromByteArray(iBytes.slice(propositionLen, propositionLen + 8))
    val dataLen: Short = Shorts.fromByteArray(iBytes.slice(propositionLen + 8, propositionLen + 8 + 2))
    val data: Array[BxTypeId] = iBytes.takeRight(dataLen)
    DataBox(proposition, nonce, data)
  }
}
