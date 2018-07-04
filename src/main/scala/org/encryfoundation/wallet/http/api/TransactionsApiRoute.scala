package org.encryfoundation.wallet.http.api

import akka.actor.ActorRefFactory
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import org.encryfoundation.wallet.transaction.{PaymentTransactionRequest, ScriptedTransactionRequest}

case class TransactionsApiRoute(implicit val context: ActorRefFactory) extends ApiRoute {

  import org.encryfoundation.wallet.actions.TransactionActions._
  import org.encryfoundation.wallet.actions.WalletActions._

  override def route: Route = pathPrefix("transactions") {
    sendPaymentTransactionR ~ sendScriptedTransactionR
  }

  def sendPaymentTransactionR: Route = (path("send" / "payment") & hexString) { walletId =>
    post(entity(as[PaymentTransactionRequest]) { r =>
      complete {
        fromId(walletId).map { w =>
          sendPaymentTransaction(r.fee, r.amount, r.recipient)(w)
          StatusCodes.OK
        }
      }
    })
  }

  def sendScriptedTransactionR: Route = (path("send" / "scripted") & hexString) { walletId =>
    post(entity(as[ScriptedTransactionRequest]) { r =>
      complete {
        fromId(walletId).map { w =>
          sendScriptedTransaction(r.fee, r.amount, r.source)(w)
          StatusCodes.OK
        }
      }
    })
  }
}
