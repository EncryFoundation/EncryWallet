package crypto

import io.circe.{Decoder, Encoder, HCursor}
import io.circe.syntax._
import scorex.crypto.encode.Base58
import scorex.crypto.signatures.{Curve25519, Signature}
import scala.util.Try

case class Signature25519(signature: Signature) {

  lazy val bytes: Array[Byte] = Signature25519.Serializer.toBytes(this)

  def isValid(pubKey: PublicKey25519, message: Array[Byte]): Boolean =
    signature.isEmpty || signature.length == Curve25519.SignatureLength &&
      Curve25519.verify(signature, message, pubKey.pubKeyBytes)
}

object Signature25519 {

  lazy val SignatureSize: Int = Curve25519.SignatureLength

  implicit val jsonEncoder: Encoder[Signature25519] = (p: Signature25519) => Map(
    "signature" -> Base58.encode(p.signature).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[Signature25519] =
    (c: HCursor) => {
      for {
        sig <- c.downField("signature").as[String]
      } yield {
        Signature25519(Signature @@ Base58.decode(sig).get)
      }
    }

  object Serializer {
    def toBytes(obj: Signature25519): Array[Byte] = obj.signature
    def parseBytes(bytes: Array[Byte]): Try[Signature25519] = Try(Signature25519(Signature @@ bytes))
  }
}
