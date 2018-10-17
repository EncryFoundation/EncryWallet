package models

import com.google.common.primitives.{Bytes, Longs}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.crypto.{PrivateKey25519, PublicKey25519, Signature25519}
import org.encryfoundation.common.transaction.{Input, Proof, PubKeyLockedContract}
import org.encryfoundation.prismlang.compiler.CompiledContract
import org.encryfoundation.prismlang.core.wrapped.BoxedValue
import org.encryfoundation.common.Algos
import scorex.crypto.hash.{Blake2b256, Digest32}
import models.directives._
import org.encryfoundation.common.utils.TaggedTypes.ADKey

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
    "id" -> Algos.encode(tx.id).asJson,
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
                                       useOutputs: Seq[(Output, Option[(CompiledContract, Seq[Proof])])],
                                       recipient: String,
                                       amount: Long,
                                       tokenIdOpt: Option[ADKey] = None): EncryTransaction = {
    val transferDirective: TransferDirective = TransferDirective(recipient, amount, tokenIdOpt)
    prepareTransaction(privKey, fee, timestamp, useOutputs, transferDirective, amount, tokenIdOpt)
  }

  def scriptedAssetTransactionScratch(privKey: PrivateKey25519,
                                      fee: Long,
                                      timestamp: Long,
                                      useOutputs: Seq[(Output, Option[(CompiledContract, Seq[Proof])])],
                                      contract: CompiledContract,
                                      amount: Long,
                                      tokenIdOpt: Option[ADKey] = None): EncryTransaction = {
    val scriptedAssetDirective: ScriptedAssetDirective = ScriptedAssetDirective(contract.hash, amount, tokenIdOpt)
    prepareTransaction(privKey, fee, timestamp, useOutputs, scriptedAssetDirective, amount, tokenIdOpt)
  }

  def assetIssuingTransactionScratch(privKey: PrivateKey25519,
                                     fee: Long,
                                     timestamp: Long,
                                     useOutputs: Seq[(Output, Option[(CompiledContract, Seq[Proof])])],
                                     contract: CompiledContract,
                                     amount: Long,
                                     tokenIdOpt: Option[ADKey] = None): EncryTransaction = {
    val assetIssuingDirective: AssetIssuingDirective = AssetIssuingDirective(contract.hash, amount)
    prepareTransaction(privKey, fee, timestamp, useOutputs, assetIssuingDirective, amount, tokenIdOpt)
  }

  def dataTransactionScratch(privKey: PrivateKey25519,
                             fee: Long,
                             timestamp: Long,
                             useOutputs: Seq[(Output, Option[(CompiledContract, Seq[Proof])])],
                             contract: CompiledContract,
                             amount: Long,
                             data: Array[Byte],
                             tokenIdOpt: Option[ADKey] = None): EncryTransaction = {
    val dataDirective: DataDirective = DataDirective(contract.hash, data)
    prepareTransaction(privKey, fee, timestamp, useOutputs, dataDirective, amount, tokenIdOpt)
  }

  private def prepareTransaction(privKey: PrivateKey25519,
                                       fee: Long,
                                       timestamp: Long,
                                       useOutputs: Seq[(Output, Option[(CompiledContract, Seq[Proof])])],
                                       directive: Directive,
                                       amount: Long,
                                       tokenIdOpt: Option[ADKey] = None): EncryTransaction = {

    val pubKey: PublicKey25519 = privKey.publicImage

    val outputs: IndexedSeq[(Output, Option[(CompiledContract, Seq[Proof])])] =
      useOutputs
        .sortWith(_._1.monetaryValue > _._1.monetaryValue)
        .foldLeft(IndexedSeq.empty[(Output, Option[(CompiledContract, Seq[Proof])])])((acc, e) =>
          if (acc.map(_._1.monetaryValue).sum < amount) acc :+ e else acc)

    val uInputs: IndexedSeq[Input] =
      outputs
        .map { case (o, co) =>
          Input.unsigned(
            Algos.decode(o.id).map(ADKey @@ _)
              .getOrElse(throw new RuntimeException(s"Output id ${o.id} can not be decoded with Base16")),
            co match {
              case Some((ct, _)) => Left(ct)
              case None => Right(PubKeyLockedContract(pubKey.pubKeyBytes))
            }
          )
        }

    val change: Long = outputs.map(_._1.monetaryValue).sum - (amount + fee)

    if (change < 0) throw new RuntimeException("Transaction impossible: required amount is bigger than available")

    val directives: IndexedSeq[Directive] =
      if (change > 0) IndexedSeq(directive, TransferDirective(pubKey.address.address, change, tokenIdOpt))
      else IndexedSeq(directive)

    val uTransaction: UnsignedEncryTransaction = UnsignedEncryTransaction(fee, timestamp, uInputs, directives)
    val signature: Signature25519 = privKey.sign(uTransaction.messageToSign)

    val proofs: IndexedSeq[Seq[Proof]] = useOutputs.flatMap(_._2.map(_._2)).toIndexedSeq

    uTransaction.toSigned(proofs, Some(Proof(BoxedValue.Signature25519Value(signature.bytes.toList))))
  }

}
