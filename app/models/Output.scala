package models

case class Output(id: String,
                  txId: String,
                  monetaryValue: Long,
                  coinId: String,
                  contractHash: String,
                  data: String)

object Output {

  import io.circe.{Decoder, Encoder, HCursor}
  import io.circe.syntax._

  implicit val jsonEncoder: Encoder[Output] = (o: Output) => Map(
    "id" -> o.id.asJson,
    "txId" -> o.txId.asJson,
    "value" -> o.monetaryValue.asJson,
    "coinId" -> o.coinId.asJson,
    "contractHash" -> o.contractHash.asJson,
    "data" -> o.data.asJson
  ).asJson

  implicit val jsonDecoder: Decoder[Output] = (c: HCursor) => for {
    id <- c.downField("id").as[String]
    txId <- c.downField("txId").as[String]
    monetaryValue <- c.downField("value").as[Long]
    coinId <- c.downField("coinId").as[String]
    contractHash <- c.downField("contractHash").as[String]
    data <- c.downField("data").as[String]
  } yield Output(
    id,
    txId,
    monetaryValue,
    coinId,
    contractHash,
    data
  )
}
