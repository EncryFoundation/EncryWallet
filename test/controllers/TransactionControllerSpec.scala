package controllers

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.scalatest.Matchers._
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.mvc.Result
import play.api.test.{FakeRequest, Injecting}
import play.api.test.Helpers._
import models._
import services.TransactionService

class TransactionControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  "TransactionController#sendPaymentTransaction" should {

    val samplePaymentTransactionRequest: PaymentTransactionRequest = PaymentTransactionRequest(1L, 2L, "Blablabla")

    "handle OK responses from explorer" in {
      val mockTransactionService: TransactionService = mock[TransactionService]
      when(mockTransactionService.sendPaymentTransaction(anyString, any[PaymentTransactionRequest])) thenReturn Future.successful(HttpResponse(StatusCodes.OK))
      val wc: TransactionController = new TransactionController()(inject[ExecutionContext], mockTransactionService, stubControllerComponents())
      val result: Future[Result] = wc.sendPaymentTransaction("Blablabla").apply(FakeRequest().withBody(samplePaymentTransactionRequest))

      status(result) shouldBe OK
    }

    "handle not OK responses from explorer" in {
      val mockTransactionService: TransactionService = mock[TransactionService]
      when(mockTransactionService.sendPaymentTransaction(anyString, any[PaymentTransactionRequest])) thenReturn Future.successful(HttpResponse(StatusCodes.InternalServerError))
      val wc: TransactionController = new TransactionController()(inject[ExecutionContext], mockTransactionService, stubControllerComponents())
      val result: Future[Result] = wc.sendPaymentTransaction("Blablabla").apply(FakeRequest().withBody(samplePaymentTransactionRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "handle failed requests from service" in {
      val mockTransactionService: TransactionService = mock[TransactionService]
      when(mockTransactionService.sendPaymentTransaction(anyString, any[PaymentTransactionRequest])) thenReturn Future.failed(new RuntimeException("Oops! Something went wrong!"))
      val wc: TransactionController = new TransactionController()(inject[ExecutionContext], mockTransactionService, stubControllerComponents())
      val result: Future[Result] = wc.sendPaymentTransaction("Blablabla").apply(FakeRequest().withBody(samplePaymentTransactionRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "handle failed request on invalid  walled id" in {
      val mockTransactionService: TransactionService = mock[TransactionService]
      when(mockTransactionService.sendPaymentTransaction(anyString, any[PaymentTransactionRequest])) thenReturn Future.failed(new IllegalArgumentException("Oops! Something went wrong!"))
      val wc: TransactionController = new TransactionController()(inject[ExecutionContext], mockTransactionService, stubControllerComponents())
      val result: Future[Result] = wc.sendPaymentTransaction("Blablabla").apply(FakeRequest().withBody(samplePaymentTransactionRequest))

      status(result) shouldBe BAD_REQUEST
    }

  }

  "TransactionController#sendScriptedTransaction" should {

    val sampleScriptedTransactionRequest: ScriptedTransactionRequest = ScriptedTransactionRequest(1L, 2L, "Blablabla")

    "handle OK responses from explorer" in {
      val mockTransactionService: TransactionService = mock[TransactionService]
      when(mockTransactionService.sendScriptedTransaction(anyString, any[ScriptedTransactionRequest])) thenReturn Future.successful(HttpResponse(StatusCodes.OK))
      val wc: TransactionController = new TransactionController()(inject[ExecutionContext], mockTransactionService, stubControllerComponents())
      val result: Future[Result] = wc.sendScriptedTransaction("Blablabla").apply(FakeRequest().withBody(sampleScriptedTransactionRequest))

      status(result) shouldBe OK
    }

    "handle not OK responses from explorer" in {
      val mockTransactionService: TransactionService = mock[TransactionService]
      when(mockTransactionService.sendScriptedTransaction(anyString, any[ScriptedTransactionRequest])) thenReturn Future.successful(HttpResponse(StatusCodes.InternalServerError))
      val wc: TransactionController = new TransactionController()(inject[ExecutionContext], mockTransactionService, stubControllerComponents())
      val result: Future[Result] = wc.sendScriptedTransaction("Blablabla").apply(FakeRequest().withBody(sampleScriptedTransactionRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "handle failed requests from service" in {
      val mockTransactionService: TransactionService = mock[TransactionService]
      when(mockTransactionService.sendScriptedTransaction(anyString, any[ScriptedTransactionRequest])) thenReturn Future.failed(new RuntimeException("Oops! Something went wrong!"))
      val wc: TransactionController = new TransactionController()(inject[ExecutionContext], mockTransactionService, stubControllerComponents())
      val result: Future[Result] = wc.sendScriptedTransaction("Blablabla").apply(FakeRequest().withBody(sampleScriptedTransactionRequest))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "handle failed request on invalid  walled id" in {
      val mockTransactionService: TransactionService = mock[TransactionService]
      when(mockTransactionService.sendScriptedTransaction(anyString, any[ScriptedTransactionRequest])) thenReturn Future.failed(new IllegalArgumentException("Oops! Something went wrong!"))
      val wc: TransactionController = new TransactionController()(inject[ExecutionContext], mockTransactionService, stubControllerComponents())
      val result: Future[Result] = wc.sendScriptedTransaction("Blablabla").apply(FakeRequest().withBody(sampleScriptedTransactionRequest))

      status(result) shouldBe BAD_REQUEST
    }

  }

}
