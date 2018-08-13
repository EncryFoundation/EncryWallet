package controllers

import fastparse.core.ParseError
import models.ParsedInput
import org.encryfoundation.common.Algos
import org.encryfoundation.common.transaction.Proof
import scorex.crypto.encode.Base58
import org.encryfoundation.prismlang.compiler.{CompiledContract, PCompiler}
import org.encryfoundation.prismlang.core.wrapped.BoxedValue
import org.encryfoundation.prismlang.core.wrapped.BoxedValue._
import org.scalatest.EitherValues
import org.scalatest.Matchers._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

class ViewControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar with EitherValues {

  "ViewController#parseInputs" should {

    "handle an empty string" in {
      ViewController.parseInputs("").right.value shouldBe empty
    }

    "parse an input" in {

      val contractSource =
        """|contract (sig: Signature25519, tx: Transaction, state: State) = {
           |  let pubKey = base16'e49e7e31ce52f75efcdd6c91996028fe57c5f9f3302342d495653d856f68df22'
           |  checkSig(sig, tx.messageToSign, pubKey) && state.height > 90
           |}""".stripMargin

      val input: String =
        s"""|013d9ac1455a6f67cb1b4614153b6e1b4d566b1380e742fffb43421952d7c220
            |>>>>>>
            |$contractSource
            |------
            |019ea638a100c16202b4cfb86da9cede0c634cf4ad70bfff3afe6d58e8f1ff8b
            |------
            |012068c460d58e8670f8adc156deed82e8dec87a98ad57438c9e38a195c29007
            |------
            |011d922232b1597aea9a9f9ec92ae7e59b0906a840d13c38e232ab910b554029
            |>>>>>>
            |$contractSource
            |>>>>>>
            |sig:Signature25519Value(hex'047dfb95c380d0e98faa66c6f2118669725cc91fe60dce7e8d53b9337a6eb2a0')
            |""".stripMargin.stripLineEnd.trim

      val contract: CompiledContract = PCompiler.compile(contractSource).get

      val res: Seq[ParsedInput] = ViewController.parseInputs(input).right.value

      res.map(_.key).map(Algos.encode) shouldBe
        Seq(
          "013d9ac1455a6f67cb1b4614153b6e1b4d566b1380e742fffb43421952d7c220",
          "019ea638a100c16202b4cfb86da9cede0c634cf4ad70bfff3afe6d58e8f1ff8b",
          "012068c460d58e8670f8adc156deed82e8dec87a98ad57438c9e38a195c29007",
          "011d922232b1597aea9a9f9ec92ae7e59b0906a840d13c38e232ab910b554029"
        )
      res(0).contract shouldBe Some(contract -> Seq.empty)
      res(1).contract shouldBe None
      res(2).contract shouldBe None
      res(3).contract shouldBe Some(contract ->
        Seq(Proof(Signature25519Value(Algos.decode("047dfb95c380d0e98faa66c6f2118669725cc91fe60dce7e8d53b9337a6eb2a0").get.toList), Some("sig")))
      )
    }

  }

  "ViewController#parseScriptArgs" should {

    "handle an empty string" in {
      ViewController.parseScriptArgs("").right.value shouldBe empty
    }

    "parse script arguments" in {
      val s: String =
        """|qty:IntValue(1234);
           |b:ByteValue(23);
           |b1:BoolValue(true);
           |b2:BoolValue(false);
           |_:StringValue('34@5$zsFH34');
           |secret1:ByteCollectionValue(hex'047dfb95c380d0e98faa66c6f2118669725cc91fe60dce7e8d53b9337a6eb2a0');
           |secret2:ByteCollectionValue(base58'4Etkd64NNYEDt8TZ21Z3jNHPvfbvEksmuuTwRUtPgqGH');
           |key:Signature25519Value(hex'047dfb95c380d0e98faa66c6f2118669725cc91fe60dce7e8d53b9337a6eb2a0');
        """.stripMargin.replaceAll("\n", "")

      val expected: Seq[(String, BoxedValue)] = Seq(
        "qty" -> IntValue(1234L),
        "b" -> ByteValue(23),
        "b1" -> BoolValue(true),
        "b2" -> BoolValue(false),
        "_" -> StringValue("34@5$zsFH34"),
        "secret1" -> ByteCollectionValue(Algos.decode("047dfb95c380d0e98faa66c6f2118669725cc91fe60dce7e8d53b9337a6eb2a0").get.toList),
        "secret2" -> ByteCollectionValue(Base58.decode("4Etkd64NNYEDt8TZ21Z3jNHPvfbvEksmuuTwRUtPgqGH").get.toList),
        "key" -> Signature25519Value(Algos.decode("047dfb95c380d0e98faa66c6f2118669725cc91fe60dce7e8d53b9337a6eb2a0").get.toList)
      )
      ViewController.parseScriptArgs(s).right.value shouldBe expected
    }

    "fail on invalid input" in {
      ViewController.parseScriptArgs("b1:BoolValue(truefalse);").left.value shouldBe a[ParseError[_, _]]
      ViewController.parseScriptArgs("qty:IntValue(12abc34)").left.value shouldBe a[ParseError[_, _]]
      ViewController.parseScriptArgs("str:StringValue(1234)").left.value shouldBe a[ParseError[_, _]]
      ViewController.parseScriptArgs("qty:IntValue(12abc34)").left.value shouldBe a[ParseError[_, _]]
      ViewController.parseScriptArgs("secret1:ByteCollectionValue(hex'ABC')").left.value shouldBe a[ParseError[_, _]]
      ViewController.parseScriptArgs("secret1:ByteCollectionValue(base16'ABC')").left.value shouldBe a[ParseError[_, _]]
      ViewController.parseScriptArgs("secret1:Signature25519Value(hex'ABC')").left.value shouldBe a[ParseError[_, _]]
      ViewController.parseScriptArgs("secret1:Signature25519Value(base16'ABC')").left.value shouldBe a[ParseError[_, _]]
      ViewController.parseScriptArgs("secret1:Signature25519Value(base58'ABC')").left.value shouldBe a[ParseError[_, _]]
    }

  }

}
