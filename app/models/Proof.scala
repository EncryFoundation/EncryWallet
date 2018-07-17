package models

import java.nio.charset.Charset

import com.google.common.primitives.{Bytes, Shorts}
import org.encryfoundation.prismlang.codec.PCodec
import org.encryfoundation.prismlang.core.wrapped.BoxedValue
import scodec.bits.BitVector
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._
import scorex.crypto.encode.Base58

import scala.util.Try

case class Proof(value: BoxedValue, tagOpt: Option[String]) {

  lazy val bytes: Array[Byte] = Proof.Serializer.toBytes(this)
}

object Proof {

  def apply(value: BoxedValue): Proof = Proof(value, None)

  implicit val jsonEncoder: Encoder[Proof] = (p: Proof) => Map(
    "serializedValue" -> Base58.encode(PCodec.boxedValCodec.encode(p.value).require.toByteArray).asJson,
    "tag" -> p.tagOpt.map(_.asJson).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[Proof] = (c: HCursor) => {
    for {
      serializedValue <- c.downField("serializedValue").as[String]
      tag <- c.downField("tag").as[Option[String]]
    } yield {
      Base58.decode(serializedValue)
        .map(bytes => PCodec.boxedValCodec.decode(BitVector(bytes)).require.value)
        .map(value => Proof(value, tag)).getOrElse(throw new Exception("Decoding failed"))
    }
  }

  object Serializer {

    def toBytes(obj: Proof): Array[Byte] = {
      val valueBytes: Array[Byte] = PCodec.boxedValCodec.encode(obj.value).require.toByteArray
      Bytes.concat(
        Shorts.toByteArray(valueBytes.length.toShort),
        valueBytes,
        obj.tagOpt.map(_.getBytes(Charset.defaultCharset)).getOrElse(Array.empty)
      )
    }

    def parseBytes(bytes: Array[Byte]): Try[Proof] = Try {
      val valueLen: Short = Shorts.fromByteArray(bytes.take(2))
      val value: BoxedValue = PCodec.boxedValCodec.decode(BitVector(bytes.slice(2, valueLen + 2))).require.value
      val tagOpt: Option[String] = if (bytes.lengthCompare(valueLen + 2) != 0)
        Some(new String(bytes.drop(valueLen + 2), Charset.defaultCharset)) else None
      Proof(value, tagOpt)
    }
  }
}
