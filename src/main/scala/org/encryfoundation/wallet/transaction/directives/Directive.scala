package org.encryfoundation.wallet.transaction.directives

import io.circe._
import org.encryfoundation.wallet.transaction.box.EncryBox
import scorex.crypto.hash.Digest32

trait Directive {

  val typeId: Byte

  val cost: Long

  val isValid: Boolean

  lazy val bytes: Array[Byte] = this match {
    case td: TransferDirective => TransferDirective.Serializer.toBytes(td)
    case sad: ScriptedAssetDirective => ScriptedAssetDirective.Serializer.toBytes(sad)
  }

  def boxes(digest: Digest32, idx: Int): Seq[EncryBox]
}

object Directive {

  type DTypeId = Byte

  implicit val jsonEncoder: Encoder[Directive] = {
    case td: TransferDirective => TransferDirective.jsonEncoder(td)
    //case aid: AssetIssuingDirective => AssetIssuingDirective.jsonEncoder(aid)
    case sad: ScriptedAssetDirective => ScriptedAssetDirective.jsonEncoder(sad)
    case _ => throw new Exception("Incorrect directive type")
  }

  implicit val jsonDecoder: Decoder[Directive] = {
    Decoder.instance { c =>
      c.downField("typeId").as[DTypeId] match {
        case Right(s) => s match {
          case TransferDirective.TypeId => TransferDirective.jsonDecoder(c)
          //case AssetIssuingDirective.TypeId => AssetIssuingDirective.jsonDecoder(c)
          case ScriptedAssetDirective.TypeId => ScriptedAssetDirective.jsonDecoder(c)
          case _ => Left(DecodingFailure("Incorrect typeId", c.history))
        }
        case Left(_) => Left(DecodingFailure("None typeId", c.history))
      }
    }
  }
}
