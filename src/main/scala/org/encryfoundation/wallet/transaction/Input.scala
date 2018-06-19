package org.encryfoundation.wallet.transaction

import com.google.common.primitives.Shorts
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base58
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}

import scala.util.Try

case class Input(boxId: ADKey, proofs: List[Proof]) {

  lazy val bytesWithoutProof: Array[Byte] = boxId

  def isUnsigned: Boolean = proofs.isEmpty
}

object Input {

  def unsigned(boxId: ADKey): Input = Input(boxId, List.empty)

  implicit val jsonEncoder: Encoder[Input] = (u: Input) => Map(
    "boxId" -> Base58.encode(u.boxId).asJson,
    "proofs" -> u.proofs.map(_.asJson).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[Input] = (c: HCursor) => {
    for {
      boxId <- c.downField("boxId").as[String]
      proofs <- c.downField("proofs").as[List[Proof]]
    } yield {
      Input(ADKey @@ Base58.decode(boxId).getOrElse(throw new Exception("Json decoding failed")), proofs)
    }
  }

  object Serializer {

    def toBytes(obj: Input): Array[Byte] = if (obj.isUnsigned) obj.boxId else {
      val proofsBytes: Array[Byte] = obj.proofs.foldLeft(Array.empty[Byte]) { case (acc, proof) =>
        val proofBytes: Array[Byte] = Proof.Serializer.toBytes(proof)
        acc ++ Shorts.toByteArray(proofBytes.length.toShort) ++ proofBytes
      }
      obj.boxId ++ Array(obj.proofs.size.toByte) ++ proofsBytes
    }

    def parseBytes(bytes: Array[Byte]): Try[Input] = Try {
      if (bytes.lengthCompare(32) == 0) Input(ADKey @@ bytes, List.empty)
      else {
        val boxId: ADKey = ADKey @@ bytes.take(32)
        val proofsQty: Int = bytes.drop(32).head
        val (proofs, _) = (0 to proofsQty).foldLeft(List.empty[Proof], bytes.drop(32 + 1)) { case ((acc, bytesAcc), _) =>
          val proofLen: Int = Shorts.fromByteArray(bytesAcc.take(2))
          val proof: Proof = Proof.Serializer.parseBytes(bytesAcc.slice(2, proofLen + 2)).getOrElse(throw new Exception("Serialization failed"))
          (acc :+ proof) -> bytesAcc.drop(proofLen + 2)
        }
        Input(boxId, proofs)
      }
    }
  }
}
