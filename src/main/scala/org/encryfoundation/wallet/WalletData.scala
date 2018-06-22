package org.encryfoundation.wallet

import org.encryfoundation.wallet.Page._
import org.encryfoundation.wallet.crypto.PublicKey25519
import scalatags.Text
import scalatags.Text.all._
import scorex.crypto.encode.Base58

case class TransactionM(address: String, amount: Long, fee: Long, change: Long)
case class TransactionHistory(transactions: Seq[TransactionM] = Seq.empty){
  val view: Text.TypedTag[String] = table(cls:="table table-striped")(
    thead(
      tr(
        th("Address"),
        th("Amount"),
        th("Fee"),
        th("Change"),
      ),tbody(
        transactions.map{
          case TransactionM(address, amount, fee, change) =>
            tr(td(address), td(amount), td(fee), td(change))
        }
      )
    )
  )
}

import org.encryfoundation.wallet.utils.ExtUtils._
case class WalletData( wallet: Option[Wallet], user2: String,
                 transactionHistory: TransactionHistory = TransactionHistory()) {
  def secretKey: String = wallet.map(_.getSecret.privKeyBytes).map(Base58.encode(_)).getOrElse("noKey").toString.trace


  lazy val privateKeyInput = div( cls:="form-group")(
    label(`for`:="exampleCurrencyInput")("Private key: ", secretKey),
//    input(tpe:="text", cls:="form-control", id:="exampleCurrencyInput", name:="prKey", disabled,
//      value:=secretKey)
  )
  lazy val feeInput = div( cls:="form-group")(
    label(cls:="col-sm-3")(`for`:="exampleCurrencyInput")("Fee"),
    input(cls:="col-sm-9")(tpe:="number", cls:="form-control", id:="exampleFeeInput", placeholder:="0", name:="fee")
  )
  lazy val amountInput = div( cls:="form-group")(
    label(cls:="col-sm-3")(`for`:="exampleCurrencyInput")("Amount"),
    input(cls:="col-sm-9")(tpe:="number", cls:="form-control", id:="exampleAmountInput", placeholder:="0", name:="amount")
  )
  lazy val feeAndAmount = div( cls:="form-group")(
    div(
      label(cls:="col-sm-2")(`for`:="exampleCurrencyInput", attr("controlLabel").empty)("Fee"),
      input(cls:="col-sm-2")(tpe:="number", cls:="form-control", id:="exampleFeeInput", size:=5, placeholder:="0", name:="fee"),
      label(cls:="col-sm-2")(`for`:="exampleCurrencyInput")("Amount"),
      input(cls:="col-sm-2")(tpe:="number", cls:="form-control", id:="exampleAmountInput", size:=5, placeholder:="0", name:="amount")
    )
  )
  lazy val addressInput = div( cls:="form-group")(
    label(`for`:="exampleAddressInput")("Address"),
    input(tpe:="text", cls:="form-control", id:="exampleAddressInput", name:="recipient", value:=user2.toString),
  )
  lazy val publicKeyInput = div( cls:="form-group")(
    label(`for`:="exampleAddressInput")("Public Key: ", wallet.map(_.account.pubKey).map(Base58.encode).getOrElse("-").toString),
//    input(tpe:="text", cls:="form-control", id:="exampleAddressInput")(
//      value:=wallet.map(_.account.pubKey).map(Base58.encode).getOrElse("-").toString),
  )
  lazy val contractInput = div( cls:="form-group")(
    label(`for`:="exampleContractInput")("Contract"),
    textarea(tpe:="text", rows:=12, cls:="form-control", id:="exampleContractInput", placeholder:="ScriptCode",name:="src"),
  )


  val id1 = "transaction1"
  val id2 = "transaction2"
  val settingsId = "accountSettings"
  lazy val view = page(layout,
    modal( "Transfer", id1, form(action:="/send/address")(
      privateKeyInput, feeInput, amountInput, addressInput, submitButton)),
    modal( "Transfer with contract", id2, form(action:="/send/contract")(
      privateKeyInput, publicKeyInput, feeInput, amountInput, contractInput, submitButton)),
    modal("Account Settings", settingsId, accountSettingsForm(action:="/settings"))
  )

  lazy val layout = div(cls:="container-fluid")(
    div(cls:="row")(
      div(cls:="col-3")(
        h1("Encry Wallet"),
        div(cls:="btn-group-vertical")(
          modalButton(id1)(id1),
          modalButton(id2)(id2),
          button(tpe:="button", cls:="btn btn-outline-warning", attr("data-toggle"):="modal",
            attr("data-target"):=s"#$settingsId")("Account Settings")
        ),
      ),
      div(cls:="col-9")(transactionHistory.view),
    )
  )

  lazy val accountSettingsForm = form(
    div( cls:="form-group")(
      label(`for`:="privateKeyInput")(s"Private key: ${secretKey}"),
      input(tpe:="text", cls:="form-control", id:="privateKeyInput", placeholder:="Generate New Key or input", name:="privateKey")
    ),
    button(cls:="btn btn-outline-warning", tpe:="submit")("Set Private Key")
  )

  def modal(title: String, modalId: String, formBody: Modifier): Modifier =
    div(cls:="modal", id:=modalId)(
      div(cls:="modal-dialog")(
        div(cls:="modal-content")(
          div(cls:="modal-header")(
            h4(cls:="modal-title")(title),
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