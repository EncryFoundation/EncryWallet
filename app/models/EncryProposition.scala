package models

import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import io.iohk.iodb.ByteArrayWrapper
import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import scorex.crypto.encode.Base16
import scala.util.{Failure, Success, Try}

case class EncryProposition(contractHash: ContractHash) {

  lazy val bytes: Array[Byte] = EncryProposition.Serializer.toBytes(this)

  def isOpen: Boolean = ByteArrayWrapper(contractHash) == ByteArrayWrapper(EncryProposition.open.contractHash)

  def isHeightLockedAt(height: Int): Boolean =
    ByteArrayWrapper(contractHash) == ByteArrayWrapper(EncryProposition.heightLocked(height).contractHash)

  def isLockedByAccount(account: String): Boolean =
    ByteArrayWrapper(contractHash) == ByteArrayWrapper(EncryProposition.accountLock(account).contractHash)
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
  def accountLock(account: Account): EncryProposition = EncryProposition(AccountLockedContract(account).contract.hash)
  def accountLock(address: String): EncryProposition = accountLock(Account(address))

  object Serializer {
    def toBytes(obj: EncryProposition): Array[Byte] = obj.contractHash
    def parseBytes(bytes: Array[Byte]): Try[EncryProposition] =
      if (bytes.lengthCompare(32) == 0) Success(EncryProposition(bytes))
      else Failure(new Exception("Invalid contract hash length"))
  }
}
