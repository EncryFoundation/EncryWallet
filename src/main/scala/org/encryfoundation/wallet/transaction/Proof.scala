package org.encryfoundation.wallet.transaction

import org.encryfoundation.prismlang.codec.PCodec
import org.encryfoundation.prismlang.core.wrapped.BoxedValue
import scodec.bits.BitVector
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._
import scorex.crypto.encode.Base58

case class Proof(value: BoxedValue, tagOpt: Option[String])

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
}
