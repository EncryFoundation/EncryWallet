import io.circe.parser._
import org.encryfoundation.wallet.transaction.box.AssetBox
import org.encryfoundation.wallet.transaction.box.AssetBox._
import org.encryfoundation.wallet.utils.ExtUtils._
import org.scalatest._

class JsonSerializerTest  extends PropSpec with Matchers {

  property("Asset Boxes from node"){}

  property("Asset box decoder test") {

    val json1 =
      """
        |    {
        |      "nonce" : 2570520101091082810,
        |      "id" : "8GgKiSczkS1GPa2h38SxWQt4KkEt6bGd5dx1V8yRvnb",
        |      "tokenId" : null,
        |      "type" : 1,
        |      "proposition" : {
        |        "script" : "SNpbTW2noppcART1wHe6mHLqYtajiiWwFvXbnaP35kZShimrfKEjoXDpsbmrNc3ToVLQHXcwfGzG5XdKHPdXXQUgQud42N6h4M4UKttcwxJT5GfurTQJDq4gKCdVVqJknFrfvDLr5azFacz8217Gsmj2hge1m1B7mahDHRzZ3Y471pwHrDGspbiuyQCvvd7jiWMupmxKJ8483wVjjXUFck4Qg94SoYJmnubjNAuwv1DJmg"
        |      },
        |      "value" : 1989800
        |    },
      """.stripMargin

    decode[AssetBox](json1).trace

    val json =
      """[
        |    {
        |      "nonce" : -8506939166499002754,
        |      "id" : "8fkABbU9oR6MJyuQFp2usiXGc8fdUufjY1XYEqeuUYN",
        |      "tokenId" : null,
        |      "type" : 1,
        |      "proposition" : {
        |        "script" : "SNpbTW2noppcART1wHe6mHLqYtajiiWwFvXbnaP35kZShimrfKEjoXDpsbmrNc3ToVLQHXcwfGzG5XdKHPdXXQUgQud42N6h4M4UKttcwxJT5GfurTQJDq4gKCdVVqJknFrfvDLr5azFacz8217Gsmj2hge1m1B7mahDHRzZ3Y471pwHrDGspbiuyQCvvd7jiWMupmxKJ8483wVjjXUFck4Qg94SoYJmnubjNAuwv1DJmg"
        |      },
        |      "value" : 1969700
        |    },
        |    {
        |      "nonce" : 7542729214514099406,
        |      "id" : "8Lraq7YnZL3Me2KTGgxuAT4cCaGuQtdGEd2Xy2uvBR8",
        |      "tokenId" : null,
        |      "type" : 1,
        |      "proposition" : {
        |        "script" : "SNpbTW2noppcART1wHe6mHLqYtajiiWwFvXbnaP35kZShimrfKEjoXDpsbmrNc3ToVLQHXcwfGzG5XdKHPdXXQUgQud42N6h4M4UKttcwxJT5GfurTQJDq4gKCdVVqJknFrfvDLr5azFacz8217Gsmj2hge1m1B7mahDHRzZ3Y471pwHrDGspbiuyQCvvd7jiWMupmxKJ8483wVjjXUFck4Qg94SoYJmnubjNAuwv1DJmg"
        |      },
        |      "value" : 1989800
        |    },
        |    {
        |      "nonce" : 2903136069779126945,
        |      "id" : "5Emjt8HtPpm99n4kigxybJWw5phfFshBpskMb6TguCT",
        |      "tokenId" : null,
        |      "type" : 1,
        |      "proposition" : {
        |        "script" : "SNpbTW2noppcART1wHe6mHLqYtajiiWwFvXbnaP35kZShimrfKEjoXDpsbmrNc3ToVLQHXcwfGzG5XdKHPdXXQUgQud42N6h4M4UKttcwxJT5GfurTQJDq4gKCdVVqJknFrfvDLr5azFacz8217Gsmj2hge1m1B7mahDHRzZ3Y471pwHrDGspbiuyQCvvd7jiWMupmxKJ8483wVjjXUFck4Qg94SoYJmnubjNAuwv1DJmg"
        |      },
        |      "value" : 2009900
        |    },
        |    {
        |      "nonce" : -2684592946267107158,
        |      "id" : "7W9YdQgm6UdpKfhy7JJ3P4yLUNN32ERPG6yBDsEwWoH",
        |      "tokenId" : null,
        |      "type" : 1,
        |      "proposition" : {
        |        "script" : "SNpbTW2noppcART1wHe6mHLqYtajiiWwFvXbnaP35kZShimrfKEjoXDpsbmrNc3ToVLQHXcwfGzG5XdKHPdXXQUgQud42N6h4M4UKttcwxJT5GfurTQJDq4gKCdVVqJknFrfvDLr5azFacz8217Gsmj2hge1m1B7mahDHRzZ3Y471pwHrDGspbiuyQCvvd7jiWMupmxKJ8483wVjjXUFck4Qg94SoYJmnubjNAuwv1DJmg"
        |      },
        |      "value" : 2030000
        |    },
        |    {
        |      "nonce" : 2948104228829592896,
        |      "id" : "4w2niUKhtgJNmr3ykC5yDT1XoSxwL9xpevKsQuZ1Muf",
        |      "tokenId" : null,
        |      "type" : 1,
        |      "proposition" : {
        |        "script" : "SNpbTW2noppcART1wHe6mHLqYtajiiWwFvXbnaP35kZShimrfKEjoXDpsbmrNc3ToVLQHXcwfGzG5XdKHPdXXQUgQud42N6h4M4UKttcwxJT5GfurTQJDq4gKCdVVqJknFrfvDLr5azFacz8217Gsmj2hge1m1B7mahDHRzZ3Y471pwHrDGspbiuyQCvvd7jiWMupmxKJ8483wVjjXUFck4Qg94SoYJmnubjNAuwv1DJmg"
        |      },
        |      "value" : 2009900
        |    },
        |    {
        |      "nonce" : 2570520101091082810,
        |      "id" : "8GgKiSczkS1GPa2h38SxWQt4KkEt6bGd5dx1V8yRvnb",
        |      "tokenId" : null,
        |      "type" : 1,
        |      "proposition" : {
        |        "script" : "SNpbTW2noppcART1wHe6mHLqYtajiiWwFvXbnaP35kZShimrfKEjoXDpsbmrNc3ToVLQHXcwfGzG5XdKHPdXXQUgQud42N6h4M4UKttcwxJT5GfurTQJDq4gKCdVVqJknFrfvDLr5azFacz8217Gsmj2hge1m1B7mahDHRzZ3Y471pwHrDGspbiuyQCvvd7jiWMupmxKJ8483wVjjXUFck4Qg94SoYJmnubjNAuwv1DJmg"
        |      },
        |      "value" : 1989800
        |    },
        |    {
        |      "nonce" : 2472425844085218219,
        |      "id" : "8UErsrakKns4Ht48g45eRaCDx5X1HquYv6Tfag2mj3u",
        |      "tokenId" : null,
        |      "type" : 1,
        |      "proposition" : {
        |        "script" : "SNpbTW2noppcART1wHe6mHLqYtajiiWwFvXbnaP35kZShimrfKEjoXDpsbmrNc3ToVLQHXcwfGzG5XdKHPdXXQUgQud42N6h4M4UKttcwxJT5GfurTQJDq4gKCdVVqJknFrfvDLr5azFacz8217Gsmj2hge1m1B7mahDHRzZ3Y471pwHrDGspbiuyQCvvd7jiWMupmxKJ8483wVjjXUFck4Qg94SoYJmnubjNAuwv1DJmg"
        |      },
        |      "value" : 1969700
        |    },
        |    {
        |      "nonce" : -8365166295327412158,
        |      "id" : "6uM8Urw9LDvAo6Gm8mAie4sDhRKFhUvSEpb3wnJZ6cK",
        |      "tokenId" : null,
        |      "type" : 1,
        |      "proposition" : {
        |        "script" : "SNpbTW2noppcART1wHe6mHLqYtajiiWwFvXbnaP35kZShimrfKEjoXDpsbmrNc3ToVLQHXcwfGzG5XdKHPdXXQUgQud42N6h4M4UKttcwxJT5GfurTQJDq4gKCdVVqJknFrfvDLr5azFacz8217Gsmj2hge1m1B7mahDHRzZ3Y471pwHrDGspbiuyQCvvd7jiWMupmxKJ8483wVjjXUFck4Qg94SoYJmnubjNAuwv1DJmg"
        |      },
        |      "value" : 2030000
        |    }
        |  ]""".stripMargin

    decode[Seq[AssetBox]](json).trace
  }
}
