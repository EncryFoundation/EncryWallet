import encry.account.Address
import encry.crypto._
import scalatags.Text.all._

import org.encryfoundation.wallet.Page._


case class Transaction(address: Address, amount: Long, fee: Long, change: Long){}
case class TransactionHistory(transactions: Seq[Transaction] = Seq.empty){
  val view = table(cls:="table table-striped")(
    thead(
      tr(
        th("Address"),
        th("Amount"),
        th("Fee"),
        th("Change"),
      ),tbody(
        transactions.map{
          case Transaction(address, amount, fee, change) =>
            tr(td(address), td(amount), td(fee), td(change))
        }
      )
    )
  )
}

class WalletData(val user1PrivateKey: PrivateKey25519, val user1PublicKey: PublicKey25519, val user2: Address,
                 transactionHistory: TransactionHistory = TransactionHistory()) {
  //val transactionForm =
  lazy val transactionFormInner = Seq(
    div( cls:="form-group")(
      label(`for`:="exampleCurrencyInput")("Private key:"),
      input(tpe:="text", cls:="form-control", id:="exampleCurrencyInput", name:="prKey", disabled,
        value:=user1PrivateKey.toString)
    ),
    div( cls:="form-group")(
      label(`for`:="exampleCurrencyInput")("Fee"),
      input(tpe:="number", cls:="form-control", id:="exampleFeeInput", placeholder:="0", name:="fee")
    ),
    div( cls:="form-group")(
      label(`for`:="exampleCurrencyInput")("Change"),
      input(tpe:="number", cls:="form-control", id:="exampleChangeInput", placeholder:="0", name:="change")
    ),
    div( cls:="form-group")(
      label(`for`:="exampleCurrencyInput")("Amount"),
      input(tpe:="number", cls:="form-control", id:="exampleAmountInput", placeholder:="0", name:="amount")
    ),
    div( cls:="form-group")(
      label(`for`:="exampleAddressInput")("Address"),
      input(tpe:="text", cls:="form-control", id:="exampleAddressInput", name:="recepient", value:=user2.toString),
    ),
  )

  val id1 = "transaction1"
  val id2 = "transaction2"
  def view = page(layout,
    modal( id1, form(transactionFormInner)(submitButton)),
    modal( id2, form(transactionFormInner)(contractInput,submitButton)),
  )

  lazy val layout = div(cls:="container-fluid")(
    div(cls:="row")(
      div(cls:="col-3")(
        h1("Encry Wallet"),
        div(cls:="btn-group-vertical")(
          modalButton(id1)(id1),
          modalButton(id2)(id2),
          button(tpe:="button", cls:="btn btn-outline-warning", attr("data-toggle"):="modal",
            attr("data-target"):=s"#")("Login")
        ),
      ),
      div(cls:="col-9")(transactionHistory.view),
    )
  )

  def modal(modalId: String, formBody: Modifier): Modifier =
    div(cls:="modal", id:=modalId)(
      div(cls:="modal-dialog")(
        div(cls:="modal-content")(
          div(cls:="modal-header")(
            h4(cls:="modal-title")("Transfer"),
            button(tpe:="button", cls:="close", attr("data-dissmiss"):="modal")(raw("&times;"))
          ),
          div(cls:="modal-body")(formBody),
          //          div(cls:="modal-footer")(
          //            button(tpe:="button",cls:="btn btn-primary")("Save changes"),
          //            button(tpe:="button",cls:="btn btn-secondary", attr("data-dissmiss"):="modal")("Close"),
          //          )
        )
      )
    )
}