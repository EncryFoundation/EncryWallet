package models.directives

import java.nio.charset.Charset

import com.google.common.primitives.{Bytes, Ints, Longs}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import models.box.{AssetBox, EncryBox}
import models.{EncryAddress, EncryProposition}
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base16
import scorex.crypto.hash.Digest32

import scala.util.Try

case class TransferDirective(address: String,
                             amount: Long,
                             tokenIdOpt: Option[ADKey] = None) extends Directive {

  override val typeId: Byte = TransferDirective.TypeId

  override def boxes(digest: Digest32, idx: Int): Seq[EncryBox] =
    Seq(AssetBox(EncryProposition.addressLocked(address),
      EncryBox.nonceFromDigest(digest ++ Ints.toByteArray(idx)), amount, tokenIdOpt))

  override lazy val isValid: Boolean = amount > 0 && EncryAddress.resolveAddress(address).isSuccess

  lazy val isIntrinsic: Boolean = tokenIdOpt.isEmpty
}

object TransferDirective {

  val TypeId: Byte = 1.toByte

  implicit val jsonEncoder: Encoder[TransferDirective] = (d: TransferDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "address" -> d.address.toString.asJson,
    "amount" -> d.amount.asJson,
    "tokenId" -> d.tokenIdOpt.map(id => Base16.encode(id)).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[TransferDirective] = (c: HCursor) => {
    for {
      address <- c.downField("address").as[String]
      amount <- c.downField("amount").as[Long]
      tokenIdOpt <- c.downField("tokenId").as[Option[String]]
    } yield {
      TransferDirective(
        address,
        amount,
        tokenIdOpt.flatMap(id => Base16.decode(id).map(ADKey @@ _).toOption)
      )
    }
  }

  object Serializer {
    def toBytes(obj: TransferDirective): Array[Byte] = {
      val address: Array[Byte] = obj.address.getBytes(Charset.defaultCharset())
      address.length.toByte +: Bytes.concat(
        address,
        Longs.toByteArray(obj.amount),
        obj.tokenIdOpt.getOrElse(Array.empty)
      )
    }

    def parseBytes(bytes: Array[Byte]): Try[TransferDirective] = Try {
      val addressLen: Int = bytes.head.toInt
      val address: String = new String(bytes.slice(1, 1 + addressLen), Charset.defaultCharset())
      val amount: Long = Longs.fromByteArray(bytes.slice(1 + addressLen, 1 + addressLen + 8))
      val tokenIdOpt: Option[ADKey] = if ((bytes.length - (1 + addressLen + 8)) == 32) {
        Some(ADKey @@ bytes.takeRight(32))
      } else None
      TransferDirective(address, amount, tokenIdOpt)
    }
  }
}
