package models.box

import com.google.common.primitives.Longs
import io.circe.{Decoder, DecodingFailure, Encoder}
import scorex.crypto.authds.ADKey
import scorex.crypto.hash.Blake2b256

trait EncryBox {

  val typeId: Byte

  val nonce: Long

  lazy val id: ADKey = ADKey @@ Blake2b256.hash(Longs.toByteArray(nonce)).updated(0, typeId)
}

object EncryBox {

  def nonceFromDigest(digest: Array[Byte]): Long = Longs.fromByteArray(Blake2b256.hash(digest).take(8))

  implicit val jsonEncoder: Encoder[EncryBox] = {
    case ab: AssetBox => AssetBox.jsonEncoder(ab)
//    case db: DataBox => DataBox.jsonEncoder(db)
//    case acb: AssetCreationBox => AssetCreationBox.jsonEncoder(acb)
//    case aib: AssetIssuingBox => AssetIssuingBox.jsonEncoder(aib)
  }

  implicit val jsonDecoder: Decoder[EncryBox] = {
    Decoder.instance { c =>
      c.downField("typeId").as[Byte] match {
        case Right(s) => s match {
          case AssetBox.TypeId => AssetBox.jsonDecoder(c)
          //case AssetIssuingDirective.TypeId => AssetIssuingDirective.jsonDecoder(c)
          case _ => Left(DecodingFailure("Incorrect typeId", c.history))
        }
        case Left(_) => Left(DecodingFailure("None typeId", c.history))
      }
    }
  }
}
