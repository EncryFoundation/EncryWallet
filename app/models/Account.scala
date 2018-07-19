package models

import scala.util.Try
import scorex.crypto.encode.Base58
import scorex.crypto.signatures.PublicKey
import crypto.{Base58Check, PublicKey25519}

/** Represents the owner of the Public/Private key pair. */
case class Account(address: String) {

  lazy val bytes: Array[Byte] = Account.Serializer.toBytes(this)

  lazy val isValid: Boolean = Base58Check.decode(address).map(bytes =>
    if (bytes.length != PublicKey25519.Length) throw new Exception("Invalid address")
  ).isSuccess

  lazy val pubKey: PublicKey = Account.pubKeyFromAddress(address)
    .getOrElse(throw new Exception("Invalid address"))

  override def toString: String = address
}

object Account {

  val AddressLength: Int = 1 + PublicKey25519.Length + Base58Check.ChecksumLength

  def apply(publicKey: PublicKey): Account = Account(Base58Check.encode(publicKey))

  def pubKeyFromAddress(address: String): Try[PublicKey] = Base58Check.decode(address).map(PublicKey @@ _)

  def decodeAddress(address: String): Array[Byte] = Base58.decode(address).get

  def validAddress(address: String): Boolean = Account(address).isValid

  object Serializer {
    def toBytes(obj: Account): Array[Byte] = Base58.decode(obj.address).get // TODO: .get
    def parseBytes(bytes: Array[Byte]): Try[Account] = Try(Account(Base58.encode(bytes)))
  }

}
