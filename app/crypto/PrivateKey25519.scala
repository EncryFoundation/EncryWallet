package crypto

import com.google.common.primitives.Bytes
import scorex.crypto.signatures.{Curve25519, PrivateKey, PublicKey}
import scala.util.Try

case class PrivateKey25519(privKeyBytes: PrivateKey, publicKeyBytes: PublicKey) {

  lazy val publicImage: PublicKey25519 = PublicKey25519(publicKeyBytes)

  def sign(message: Array[Byte]): Signature25519 = Signature25519(Curve25519.sign(privKeyBytes, message))
}

object PrivateKey25519 {

  def sign(secret: PrivateKey25519, message: Array[Byte]): Signature25519 =
    Signature25519(Curve25519.sign(secret.privKeyBytes, message))

  def verify(message: Array[Byte], publicImage: PublicKey25519, proof: Signature25519): Boolean =
    Curve25519.verify(proof.signature, message, publicImage.pubKeyBytes)

  def generateKeys(randomSeed: Array[Byte]): (PrivateKey25519, PublicKey25519) = {
    val pair = Curve25519.createKeyPair(randomSeed)
    val secret: PrivateKey25519 = PrivateKey25519(pair._1, pair._2)
    secret -> secret.publicImage
  }

  object Serializer {
    def toBytes(obj: PrivateKey25519): Array[Byte] = Bytes.concat(obj.privKeyBytes, obj.publicKeyBytes)
    def parseBytes(bytes: Array[Byte]): Try[PrivateKey25519] = Try {
      PrivateKey25519(PrivateKey @@ bytes.slice(0, 32), PublicKey @@ bytes.slice(32, 64))
    }
  }
}
