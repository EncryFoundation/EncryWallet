import io.circe._
import org.encryfoundation.wallet
import org.encryfoundation.wallet.transaction.EncryProposition
import org.encryfoundation.wallet.transaction.box.AssetBox
import org.scalatest._
import org.encryfoundation.wallet.utils.ExtUtils._

class JsonSerializerTest  extends PropSpec with Matchers{
//  val a = AssetBox(EncryProposition())

  implicit val decodeEncryProposition: Decoder[EncryProposition] = (c: HCursor) => for {
    address <- c.downField("address").as[String]
  } yield EncryProposition.accountLock(address)


  implicit val decodeAssetBox: Decoder[AssetBox] = (c: HCursor) => for {
    nonce <- c.downField("nonce").as[Long]
    id <- c.downField("id").as[String]
    proposition <- c.downField("proposition").as[EncryProposition]
    amount <- c.downField("value").as[Long]
  } yield new AssetBox(proposition, nonce, amount)


  val abExample = AssetBox(EncryProposition.accountLock("4NhqE33ps1mdFwiKsTXEeRHZikmvU4ha3UkJqphyeRvS3RV4Vx"),
    -1423773885618135344L, 1969300, None)

  import io.circe._, io.circe.parser._
  import io.circe._, io.circe.syntax._

  val json1 =
    """
      |{
      |    "nonce" : -1423773885618135344,
      |    "id" : "8Qh1Q7RUSopoGGfVRYvCJfJkf4nEuRYdMtXty7SCgzA",
      |    "tokenId" : null,
      |    "type" : 1,
      |    "proposition" : {
      |      "typeId" : 2,
      |      "address" : "4NhqE33ps1mdFwiKsTXEeRHZikmvU4ha3UkJqphyeRvS3RV4Vx"
      |    },
      |    "value" : 1969300
      |  }
    """.stripMargin

  decode[AssetBox](json1).trace

  val json =
    """
      |[
      |  {
      |    "nonce" : -1423773885618135344,
      |    "id" : "8Qh1Q7RUSopoGGfVRYvCJfJkf4nEuRYdMtXty7SCgzA",
      |    "tokenId" : null,
      |    "type" : 1,
      |    "proposition" : {
      |      "typeId" : 2,
      |      "address" : "4NhqE33ps1mdFwiKsTXEeRHZikmvU4ha3UkJqphyeRvS3RV4Vx"
      |    },
      |    "value" : 1969300
      |  },
      |  {
      |    "nonce" : -1273373567252329492,
      |    "id" : "5Du8D2UVd9qmqGHpne4H1LzuHE19d1qaXz4L8aEzmVR",
      |    "tokenId" : null,
      |    "type" : 1,
      |    "proposition" : {
      |      "typeId" : 2,
      |      "address" : "4NhqE33ps1mdFwiKsTXEeRHZikmvU4ha3UkJqphyeRvS3RV4Vx"
      |    },
      |    "value" : 2159400
      |  },
      |  {
      |    "nonce" : -7187492925374998615,
      |    "id" : "6sa2vdX6iihkxqdvwd1rBxmQT1RCYXQo3sTb2bKnZrK",
      |    "tokenId" : null,
      |    "type" : 1,
      |    "proposition" : {
      |      "typeId" : 2,
      |      "address" : "4NhqE33ps1mdFwiKsTXEeRHZikmvU4ha3UkJqphyeRvS3RV4Vx"
      |    },
      |    "value" : 2009500
      |  },
      |  {
      |    "nonce" : 647665066005771743,
      |    "id" : "6eGdJYzijm6zsH7hveM2nnhx6LCiGYST3NNbtH38LPu",
      |    "tokenId" : null,
      |    "type" : 1,
      |    "proposition" : {
      |      "typeId" : 2,
      |      "address" : "4NhqE33ps1mdFwiKsTXEeRHZikmvU4ha3UkJqphyeRvS3RV4Vx"
      |    },
      |    "value" : 2189600
      |  },
      |  {
      |    "nonce" : 4257005958711236845,
      |    "id" : "579Q7ufbmVSXCi8g98HUR5XprkpFH7KujDXaZ4ZjSaQ",
      |    "tokenId" : null,
      |    "type" : 1,
      |    "proposition" : {
      |      "typeId" : 2,
      |      "address" : "4NhqE33ps1mdFwiKsTXEeRHZikmvU4ha3UkJqphyeRvS3RV4Vx"
      |    },
      |    "value" : 3008300
      |  },
      |  {
      |    "nonce" : 3701934156165221680,
      |    "id" : "5xxxir8RTdgxoQSri3TXaRk3WFJj6abuQgQFcAqC5Lc",
      |    "tokenId" : null,
      |    "type" : 1,
      |    "proposition" : {
      |      "typeId" : 2,
      |      "address" : "4NhqE33ps1mdFwiKsTXEeRHZikmvU4ha3UkJqphyeRvS3RV4Vx"
      |    },
      |    "value" : 1979300
      |  },
      |  {
      |    "nonce" : -7406367754727313836,
      |    "id" : "7dMcGDkCik7t3rD6bQMfbygjhtvs38pcUkWxcbmkWBk",
      |    "tokenId" : null,
      |    "type" : 1,
      |    "proposition" : {
      |      "typeId" : 2,
      |      "address" : "4NhqE33ps1mdFwiKsTXEeRHZikmvU4ha3UkJqphyeRvS3RV4Vx"
      |    },
      |    "value" : 2859400
      |  },
      |  {
      |    "nonce" : 2056611475809368172,
      |    "id" : "7voXcZhWCb8EBVdGcqGQbs1gNcPBXAZSvhKq7xaFe31",
      |    "tokenId" : null,
      |    "type" : 1,
      |    "proposition" : {
      |      "typeId" : 2,
      |      "address" : "4NhqE33ps1mdFwiKsTXEeRHZikmvU4ha3UkJqphyeRvS3RV4Vx"
      |    },
      |    "value" : 2169500
      |  },
      |  {
      |    "nonce" : -3674053580581240723,
      |    "id" : "7h7g99ZtdBF11oHcVLQA1ufbn9Heyf77jPULFJnLk6P",
      |    "tokenId" : null,
      |    "type" : 1,
      |    "proposition" : {
      |      "typeId" : 2,
      |      "address" : "4NhqE33ps1mdFwiKsTXEeRHZikmvU4ha3UkJqphyeRvS3RV4Vx"
      |    },
      |    "value" : 2039600
      |  }
      |]
    """.stripMargin

  decode[Seq[AssetBox]](json).trace

}
