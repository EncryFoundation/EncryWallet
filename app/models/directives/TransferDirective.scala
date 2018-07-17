package models.directives

import com.google.common.primitives.{Bytes, Ints, Longs}
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._
import models.{Account, EncryProposition}
import models.box.{AssetBox, EncryBox}
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base58
import scorex.crypto.hash.Digest32

import scala.util.Try

case class TransferDirective(address: String,
                             amount: Long,
                             tokenIdOpt: Option[ADKey] = None) extends Directive {

  override val typeId: Byte = TransferDirective.TypeId

  override def boxes(digest: Digest32, idx: Int): Seq[EncryBox] =
    Seq(AssetBox(EncryProposition.accountLock(Account(address)),
      EncryBox.nonceFromDigest(digest ++ Ints.toByteArray(idx)), amount, tokenIdOpt))

  override lazy val isValid: Boolean = amount > 0 && Account.validAddress(address)

  lazy val isIntrinsic: Boolean = tokenIdOpt.isEmpty
}

object TransferDirective {

  val TypeId: Byte = 1.toByte

  implicit val jsonEncoder: Encoder[TransferDirective] = (d: TransferDirective) => Map(
    "typeId" -> d.typeId.asJson,
    "address" -> d.address.toString.asJson,
    "amount" -> d.amount.asJson,
    "tokenId" -> d.tokenIdOpt.map(id => Base58.encode(id)).asJson
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
        tokenIdOpt.flatMap(id => Base58.decode(id).map(ADKey @@ _).toOption)
      )
    }
  }

  object Serializer {
    def toBytes(obj: TransferDirective): Array[Byte] =
      Bytes.concat(
        Account.decodeAddress(obj.address),
        Longs.toByteArray(obj.amount),
        obj.tokenIdOpt.getOrElse(Array.empty)
      )

    def parseBytes(bytes: Array[Byte]): Try[TransferDirective] = Try {
      val address = Base58.encode(bytes.take(Account.AddressLength))
      val amount = Longs.fromByteArray(bytes.slice(Account.AddressLength, Account.AddressLength + 8))
      val tokenIdOpt = if ((bytes.length - (Account.AddressLength + 8)) == 32) {
        Some(ADKey @@ bytes.takeRight(32))
      } else None
      TransferDirective(address, amount, tokenIdOpt)
    }
  }
}
