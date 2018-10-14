package models

import io.circe.{Decoder, HCursor}

case class PaymentTransactionRequest(fee: Long, amount: Long, recipient: String)

object PaymentTransactionRequest {

  implicit val jsonDecoder: Decoder[PaymentTransactionRequest] = (c: HCursor) => {
    for {
      fee <- c.downField("fee").as[Long]
      amount <- c.downField("amount").as[Long]
      rec <- c.downField("recipient").as[String]
    } yield PaymentTransactionRequest(fee, amount, rec)
  }
}

case class ScriptedTransactionRequest(fee: Long, amount: Long, source: String)

object ScriptedTransactionRequest {

  implicit val jsonDecoder: Decoder[ScriptedTransactionRequest] = (c: HCursor) => {
    for {
      fee <- c.downField("fee").as[Long]
      amount <- c.downField("amount").as[Long]
      script <- c.downField("script").as[String]
    } yield ScriptedTransactionRequest(fee, amount, script)
  }
}

case class AssetIssuingTransactionRequest(fee: Long, amount: Long, source: String)

case class DataTransactionRequest(fee: Long, amount: Long, data: String)

object DataTransactionRequest {

  implicit val jsonDecoder: Decoder[DataTransactionRequest] = (c: HCursor) => {
    for {
      fee <- c.downField("fee").as[Long]
      amount <- c.downField("amount").as[Long]
      data <- c.downField("data").as[String]
    } yield DataTransactionRequest(fee, amount, data)
  }
}