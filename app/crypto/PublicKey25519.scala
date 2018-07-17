package crypto

import scorex.crypto.encode.Base58
import scorex.crypto.signatures.{Curve25519, PublicKey}
import scala.util.Try

case class PublicKey25519(pubKeyBytes: PublicKey) {

  require(pubKeyBytes.length == Curve25519.KeyLength,
    s"Incorrect pubKey length, ${Curve25519.KeyLength} expected, ${pubKeyBytes.length} given")

  lazy val address: String = Base58Check.encode(pubKeyBytes)

  override def equals(obj: scala.Any): Boolean = obj match {
    case p: PublicKey25519 => p.pubKeyBytes sameElements pubKeyBytes
    case _ => false
  }

  override def hashCode: Int = (BigInt(pubKeyBytes) % Int.MaxValue).toInt

  override def toString: String = Base58.encode(pubKeyBytes)
}

object PublicKey25519 {

  val Length: Int = 32

  def toBytes(obj: PublicKey25519): Array[Byte] = obj.pubKeyBytes

  def parseBytes(bytes: Array[Byte]): Try[PublicKey25519] = Try(PublicKey25519(PublicKey @@ bytes))
}
