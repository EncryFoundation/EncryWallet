package models.box

import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.common.serialization.Serializer
import org.encryfoundation.common.transaction._
import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import scorex.crypto.encode.Base16
import scorex.crypto.signatures.PublicKey
import scala.util.{Failure, Success, Try}

case class EncryProposition(contractHash: ContractHash) extends Proposition {

  override type M = EncryProposition

  override def serializer: Serializer[M] = EncryPropositionSerializer
}

object EncryProposition {

  implicit val jsonEncoder: Encoder[EncryProposition] = (p: EncryProposition) => Map(
    "contractHash" -> Base16.encode(p.contractHash).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[EncryProposition] = (c: HCursor) => for {
    contractHash <- c.downField("contractHash").as[String]
  } yield Base16.decode(contractHash).map(EncryProposition.apply)
    .getOrElse(throw new Exception("Decoding failed"))

  def open: EncryProposition = EncryProposition(OpenContract.contract.hash)
  def heightLocked(height: Int): EncryProposition = EncryProposition(HeightLockedContract(height).contract.hash)
  def pubKeyLocked(pubKey: PublicKey): EncryProposition = EncryProposition(PubKeyLockedContract(pubKey).contract.hash)
  def addressLocked(address: String): EncryProposition = EncryAddress.resolveAddress(address).map {
    case p2pk: Pay2PubKeyAddress => EncryProposition(PubKeyLockedContract(p2pk.pubKey).contract.hash)
    case p2sh: Pay2ContractHashAddress => EncryProposition(p2sh.contractHash)
  }.getOrElse(throw EncryAddress.InvalidAddressException)
}

object EncryPropositionSerializer extends Serializer[EncryProposition] {
  def toBytes(obj: EncryProposition): Array[Byte] = obj.contractHash
  def parseBytes(bytes: Array[Byte]): Try[EncryProposition] =
    if (bytes.lengthCompare(32) == 0) Success(EncryProposition(bytes))
    else Failure(new Exception("Invalid contract hash length"))
}
