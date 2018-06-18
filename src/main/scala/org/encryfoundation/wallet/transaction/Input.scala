package org.encryfoundation.wallet.transaction

import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base58
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}

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
      id <- Base58.decode(boxId)
    } yield {
      Input(ADKey @@ id, proofs)
    }
  }
}
