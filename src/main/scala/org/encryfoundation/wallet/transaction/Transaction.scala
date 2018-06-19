package org.encryfoundation.wallet.transaction

import com.google.common.primitives.{Bytes, Longs}
import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._
import org.encryfoundation.wallet.transaction.directives.Directive
import scorex.crypto.encode.Base58
import scorex.crypto.hash.{Blake2b256, Digest32}

/** Completely assembled atomic state modifier. */
case class EncryTransaction(fee: Long,
                            timestamp: Long,
                            inputs: IndexedSeq[Input],
                            directives: IndexedSeq[Directive],
                            defaultProofOpt: Option[Proof]) {

  val messageToSign: Array[Byte] = UnsignedEncryTransaction.bytesToSign(fee, timestamp, inputs, directives)

  lazy val id: Array[Byte] = Blake2b256.hash(messageToSign)
}

object EncryTransaction {

  implicit val jsonEncoder: Encoder[EncryTransaction] = (tx: EncryTransaction) => Map(
    "id" -> Base58.encode(tx.id).asJson,
    "fee" -> tx.fee.asJson,
    "timestamp" -> tx.timestamp.asJson,
    "inputs" -> tx.inputs.map(_.asJson).asJson,
    "directives" -> tx.directives.map(_.asJson).asJson,
    "defaultProofOpt" -> tx.defaultProofOpt.map(_.asJson).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[EncryTransaction] = (c: HCursor) => {
    for {
      fee <- c.downField("fee").as[Long]
      timestamp <- c.downField("timestamp").as[Long]
      inputs <- c.downField("inputs").as[IndexedSeq[Input]]
      directives <- c.downField("directives").as[IndexedSeq[Directive]]
      defaultProofOpt <- c.downField("defaultProofOpt").as[Option[Proof]]
    } yield {
      EncryTransaction(
        fee,
        timestamp,
        inputs,
        directives,
        defaultProofOpt
      )
    }
  }
}

/** Unsigned version of EncryTransaction (without any
  * proofs for which interactive message is required) */
case class UnsignedEncryTransaction(fee: Long,
                                    timestamp: Long,
                                    inputs: IndexedSeq[Input],
                                    directives: IndexedSeq[Directive]) {

  val messageToSign: Array[Byte] = UnsignedEncryTransaction.bytesToSign(fee, timestamp, inputs, directives)

  def toSigned(proofs: IndexedSeq[Seq[Proof]], defaultProofOpt: Option[Proof]): EncryTransaction = {
    val signedInputs: IndexedSeq[Input] = inputs.zipWithIndex.map { case (input, idx) =>
      if (proofs.nonEmpty && proofs.lengthCompare(idx + 1) <= 0) input.copy(proofs = proofs(idx).toList) else input
    }
    EncryTransaction(fee, timestamp, signedInputs, directives, defaultProofOpt)
  }
}

object UnsignedEncryTransaction {

  def bytesToSign(fee: Long,
                  timestamp: Long,
                  inputs: IndexedSeq[Input],
                  directives: IndexedSeq[Directive]): Digest32 =
    Blake2b256.hash(Bytes.concat(
      inputs.map(_.bytesWithoutProof).foldLeft(Array[Byte]())(_ ++ _),
      directives.map(_.bytes).foldLeft(Array[Byte]())(_ ++ _),
      Longs.toByteArray(timestamp),
      Longs.toByteArray(fee)
    ))
}
