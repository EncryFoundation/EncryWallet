package models

import com.google.common.primitives.{Ints, Shorts}
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor}
import org.encryfoundation.prismlang.compiler.{CompiledContract, CompiledContractSerializer}
import scorex.crypto.authds.ADKey
import scorex.crypto.encode.Base16

import scala.util.Try

case class Input(boxId: ADKey, contract: Either[CompiledContract, RegularContract], proofs: List[Proof]) {

  lazy val bytesWithoutProof: Array[Byte] = boxId

  def isUnsigned: Boolean = proofs.isEmpty
}

object Input {

  def unsigned(boxId: ADKey, contract: RegularContract): Input = Input(boxId, Right(contract), List.empty)

  implicit val jsonEncoder: Encoder[Input] = (u: Input) => Map(
    "boxId" -> Base16.encode(u.boxId).asJson,
    "contract" -> Base16.encode(Serializer.encodeEitherCompiledOrRegular(u.contract)).asJson,
    "proofs" -> u.proofs.map(_.asJson).asJson
  ).asJson

  implicit val jsonDecoder: Decoder[Input] = (c: HCursor) => {
    for {
      boxId <- c.downField("boxId").as[String]
      contractBytes <- c.downField("contract").as[String]
      proofs <- c.downField("proofs").as[List[Proof]]
    } yield Base16.decode(contractBytes)
      .flatMap(Serializer.decodeEitherCompiledOrRegular)
      .flatMap(contract => Base16.decode(boxId).map(id => Input(ADKey @@ id, contract, proofs)))
      .getOrElse(throw new Exception("Decoding failed"))
  }

  object Serializer {

    private val CCTypeId: Byte = 98
    private val RCTypeId: Byte = 99

    def encodeEitherCompiledOrRegular(contract: Either[CompiledContract, RegularContract]): Array[Byte] =
      contract.fold(CCTypeId +: _.bytes, RCTypeId +: _.bytes)

    def decodeEitherCompiledOrRegular(bytes: Array[Byte]): Try[Either[CompiledContract, RegularContract]] = bytes.head match {
      case CCTypeId => CompiledContractSerializer.parseBytes(bytes.tail).map(Left.apply)
      case RCTypeId => RegularContract.Serializer.parseBytes(bytes.tail).map(Right.apply)
    }

    def toBytesWithoutProof(obj: Input): Array[Byte] = {
      val contractBytes: Array[Byte] = encodeEitherCompiledOrRegular(obj.contract)
      obj.boxId ++ Ints.toByteArray(contractBytes.length) ++ contractBytes
    }

    def toBytes(obj: Input): Array[Byte] =
      if (obj.isUnsigned) toBytesWithoutProof(obj) else {
        val proofsBytes: Array[Byte] = obj.proofs.foldLeft(Array.empty[Byte]) { case (acc, proof) =>
          val proofBytes: Array[Byte] = Proof.Serializer.toBytes(proof)
          acc ++ Shorts.toByteArray(proofBytes.length.toShort) ++ proofBytes
        }
        toBytesWithoutProof(obj) ++ Array(obj.proofs.size.toByte) ++ proofsBytes
      }

    def parseBytes(bytes: Array[Byte]): Try[Input] = Try {
      val boxId: ADKey = ADKey @@ bytes.take(32)
      val contractLen: Int = Ints.fromByteArray(bytes.slice(32, 32 + 4))
      boxId -> contractLen
    }.flatMap { case (boxId, contractLen) =>
      decodeEitherCompiledOrRegular(bytes.slice(32 + 4, 32 + 4 + contractLen)).map { contract =>
        val proofsQty: Int = bytes.drop(32).head
        val (proofs: List[Proof], _) = (0 until proofsQty).foldLeft(List.empty[Proof], bytes.drop(32 + 1)) { case ((acc, bytesAcc), _) =>
          val proofLen: Int = Shorts.fromByteArray(bytesAcc.take(2))
          val proof: Proof = Proof.Serializer.parseBytes(bytesAcc.slice(2, proofLen + 2)).getOrElse(throw new Exception)
          (acc :+ proof) -> bytesAcc.drop(proofLen + 2)
        }
        Input(boxId, contract, proofs)
      }
    }
  }
}
