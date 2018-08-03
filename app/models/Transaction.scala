package models

import com.google.common.primitives.{Bytes, Longs}
import crypto.{PrivateKey25519, PublicKey25519, Signature25519}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import models.directives.{Directive, ScriptedAssetDirective, TransferDirective}
import org.encryfoundation.prismlang.compiler.CompiledContract
import org.encryfoundation.prismlang.core.wrapped.BoxedValue
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base16
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
    "id" -> Base16.encode(tx.id).asJson,
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
    } yield EncryTransaction(
      fee,
      timestamp,
      inputs,
      directives,
      defaultProofOpt
    )
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
      inputs.flatMap(_.bytesWithoutProof).toArray,
      directives.flatMap(_.bytes).toArray,
      Longs.toByteArray(timestamp),
      Longs.toByteArray(fee)
    ))
}

object Transaction {

  def defaultPaymentTransactionScratch(privKey: PrivateKey25519,
                                       fee: Long,
                                       timestamp: Long,
                                       usedOutputs: Seq[Output],
                                       recipient: String,
                                       amount: Long,
                                       tokenIdOpt: Option[ADKey] = None): EncryTransaction = {
    val pubKey: PublicKey25519 = privKey.publicImage
    val uInputs: IndexedSeq[Input] = usedOutputs.map(output =>
      Input.unsigned(
        Base16.decode(output.id).map(ADKey @@ _).getOrElse(throw new RuntimeException(s"Output id ${output.id} con not be decoded with Base16")),
        PubKeyLockedContract(pubKey.pubKeyBytes))
    ).toIndexedSeq
    val change: Long = usedOutputs.map(_.monetaryValue).sum - (amount + fee)
    val directives: IndexedSeq[TransferDirective] = if (change > 0) {
      IndexedSeq(TransferDirective(recipient, amount, tokenIdOpt), TransferDirective(pubKey.address.address, change, tokenIdOpt))
    } else {
      IndexedSeq(TransferDirective(recipient, amount, tokenIdOpt))
    }

    val uTransaction: UnsignedEncryTransaction = UnsignedEncryTransaction(fee, timestamp, uInputs, directives)
    val signature: Signature25519 = privKey.sign(uTransaction.messageToSign)

    uTransaction.toSigned(IndexedSeq.empty, Some(Proof(BoxedValue.Signature25519Value(signature.bytes.toList))))
  }

  def scriptedAssetTransactionScratch(privKey: PrivateKey25519,
                                      fee: Long,
                                      timestamp: Long,
                                      usedOutputs: Seq[Output],
                                      contract: CompiledContract,
                                      amount: Long,
                                      tokenIdOpt: Option[ADKey] = None): EncryTransaction = {
    val pubKey: PublicKey25519 = privKey.publicImage
    val uInputs: IndexedSeq[Input] = usedOutputs.map(output =>
      Input.unsigned(
        Base16.decode(output.id).map(ADKey @@ _).getOrElse(throw new RuntimeException(s"Output id ${output.id} con not be decoded with Base16")),
        PubKeyLockedContract(pubKey.pubKeyBytes))
    ).toIndexedSeq
    val change: Long = usedOutputs.map(_.monetaryValue).sum - (amount + fee)
    val assetDirective: ScriptedAssetDirective = ScriptedAssetDirective(contract.hash, amount, tokenIdOpt)
    val directives: IndexedSeq[Directive] =
      if (change > 0) IndexedSeq(assetDirective, TransferDirective(pubKey.address.address, change, tokenIdOpt))
      else IndexedSeq(assetDirective)

    val uTransaction: UnsignedEncryTransaction = UnsignedEncryTransaction(fee, timestamp, uInputs, directives)
    val signature: Signature25519 = privKey.sign(uTransaction.messageToSign)

    uTransaction.toSigned(IndexedSeq.empty, Some(Proof(BoxedValue.Signature25519Value(signature.bytes.toList))))
  }

  def specialTransactionScratch(privKey: PrivateKey25519,
                                fee: Long,
                                timestamp: Long,
                                useBoxes: IndexedSeq[ADKey],
                                recipient: String,
                                amount: Long,
                                change: Long,
                                tokenIdOpt: Option[ADKey] = None): EncryTransaction = {
    val pubKey: PublicKey25519 = privKey.publicImage
    val uInputs: IndexedSeq[Input] = useBoxes.map(id => Input.unsigned(id, PubKeyLockedContract(pubKey.pubKeyBytes))).toIndexedSeq
    val transferDirective: TransferDirective = TransferDirective(recipient, amount, tokenIdOpt)
    val directives: IndexedSeq[TransferDirective] =
      if (change > 0) IndexedSeq(transferDirective, TransferDirective(pubKey.address.address, change, tokenIdOpt))
      else IndexedSeq(transferDirective)

    val uTransaction: UnsignedEncryTransaction = UnsignedEncryTransaction(fee, timestamp, uInputs, directives)
    val signature: Signature25519 = privKey.sign(uTransaction.messageToSign)

    uTransaction.toSigned(IndexedSeq.empty, Some(Proof(BoxedValue.Signature25519Value(signature.bytes.toList))))
  }
}
