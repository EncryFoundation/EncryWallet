package models

import crypto.Base58Check
import org.encryfoundation.prismlang.compiler.CompiledContract.ContractHash
import scorex.crypto.signatures.PublicKey

import scala.util.Try

sealed trait EncryAddress {
  val typePrefix: Byte
  val address: String
  lazy val decoded: Try[Array[Byte]] = Base58Check.decode(address)
  def isValid: Boolean = decoded.map(_.head == typePrefix).getOrElse(false)
}
object EncryAddress {
  case object InvalidAddressException extends Exception("Invalid address")

  def resolveAddress(address: String): Try[EncryAddress] = Base58Check.decode(address).map {
    case bytes if bytes.head == Pay2PubKeyAddress.TypePrefix => Pay2PubKeyAddress(address)
    case bytes if bytes.head == Pay2ContractHashAddress.TypePrefix => Pay2ContractHashAddress(address)
  }
}

/** P2PK - public key */
case class Pay2PubKeyAddress(address: String) extends EncryAddress {
  override val typePrefix: Byte = Pay2PubKeyAddress.TypePrefix
  def pubKey: PublicKey = decoded.map(PublicKey @@ _.tail).getOrElse(throw EncryAddress.InvalidAddressException)
  def p2ch: Pay2ContractHashAddress = Pay2ContractHashAddress(PubKeyLockedContract(pubKey))
}
object Pay2PubKeyAddress {
  val TypePrefix: Byte = 0x02
  def apply(publicKey: PublicKey): Pay2PubKeyAddress = new Pay2PubKeyAddress(Base58Check.encode(TypePrefix +: publicKey))
  def extractPubKey(address: String): Try[PublicKey] = Base58Check.decode(address).map(PublicKey @@ _.tail)
}

/** P2CH - regular contract hash */
case class Pay2ContractHashAddress(address: String) extends EncryAddress {
  override val typePrefix: Byte = Pay2ContractHashAddress.TypePrefix
  def contractHash: ContractHash = decoded.map(_.tail).getOrElse(throw EncryAddress.InvalidAddressException)
}
object Pay2ContractHashAddress {
  val TypePrefix: Byte = 0x12
  def apply(contract: PubKeyLockedContract): Pay2ContractHashAddress =
    new Pay2ContractHashAddress(Base58Check.encode(TypePrefix +: contract.contract.hash))
}

