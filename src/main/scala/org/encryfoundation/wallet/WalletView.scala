package org.encryfoundation.wallet

import org.encryfoundation.wallet.Page._
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

case class WalletView(wallet: Option[Wallet], user2: String,
                      transactionHistory: TransactionHistory = TransactionHistory(),
                      error: Option[String] = None) {
  def secretKey: String = wallet.map(_.getSecret.privKeyBytes).map(Base58.encode(_)).getOrElse("noKey").toString//.trace

  lazy val privateKeyInput: Text.TypedTag[String] = div( cls:="form-group")(
    label(`for`:="exampleCurrencyInput")("Private key: ", secretKey),
  )
  lazy val feeInput: Text.TypedTag[String] = div( cls:="form-group")(
    label(cls:="col-sm-3")(`for`:="exampleFeeInput")("Fee"),
    input(cls:="col-sm-9")(tpe:="number", cls:="form-control", id:="exampleFeeInput", placeholder:="0", name:="fee")
  )
  lazy val amountInput: Text.TypedTag[String] = div( cls:="form-group")(
    label(cls:="col-sm-3")(`for`:="exampleAmountInput")("Amount"),
    input(cls:="col-sm-9")(tpe:="number", cls:="form-control", id:="exampleAmountInput", placeholder:="0", name:="amount")
  )

  lazy val changeInput: Text.TypedTag[String] = div( cls:="form-group")(
    label(cls:="col-sm-3")(`for`:="exampleChangeInput")("change"),
    input(cls:="col-sm-9")(tpe:="number", cls:="form-control", id:="exampleChangeInput", placeholder:="0", name:="change")
  )
  lazy val feeAndAmount: Text.TypedTag[String] = div( cls:="form-group")(
    div(
      label(cls:="col-sm-2")(`for`:="exampleCurrencyInput", attr("controlLabel").empty)("Fee"),
      input(cls:="col-sm-2")(tpe:="number", cls:="form-control", id:="exampleFeeInput", size:=5, placeholder:="0", name:="fee"),
      label(cls:="col-sm-2")(`for`:="exampleCurrencyInput")("Amount"),
      input(cls:="col-sm-2")(tpe:="number", cls:="form-control", id:="exampleAmountInput", size:=5, placeholder:="0", name:="amount")
    )
  )
  lazy val boxIdInput: Text.TypedTag[String] = div( cls:="form-group")(
    label(`for`:="exampleAddressInput")("BoxId"),
    input(tpe:="text", cls:="form-control", id:="exampleAddressInput", name:="boxId", value:=user2.toString),
  )
  lazy val addressInput: Text.TypedTag[String] = div( cls:="form-group")(
    label(`for`:="exampleAddressInput")("Address"),
    input(tpe:="text", cls:="form-control", id:="exampleAddressInput", name:="recipient", value:=user2.toString),
  )
  lazy val publicKeyLabel: Text.TypedTag[String] = div( cls:="form-group")(
    label(`for`:="exampleAddressInput")("Public Key: ", wallet.map(_.account.pubKey).map(Base58.encode).getOrElse("-").toString),
  )

  lazy val publicKeyInput: Text.TypedTag[String] = div( cls:="form-group")(
    label(`for`:="exampleAddressInput")("Public Key"),
    input(tpe:="text", cls:="form-control", id:="exampleAddressInput")(
      value:=wallet.map(_.account.pubKey).map(Base58.encode).getOrElse("-").toString),
  )

  lazy val contractInput: Text.TypedTag[String] = div( cls:="form-group")(
    label(`for`:="exampleContractInput")("Contract"),
    textarea(tpe:="text", rows:=12, cls:="form-control", id:="exampleContractInput", placeholder:="ScriptCode",name:="src"),
  )

  val addressSendId = "SendToAddress"
  val boxSendId = "SendWithContract"
  val boxTransactionId = "SendWithBox"
  val settingsId = "accountSettings"

  lazy val modals: Seq[Text.all.Modifier] = Seq( modal( "Transfer", addressSendId, form(action:="/send/address")(
    privateKeyInput, feeInput, amountInput, addressInput, submitButton)),
  modal( "Transfer with contract", boxSendId, form(action:="/send/contract")(
    privateKeyInput, publicKeyLabel, feeInput, amountInput, contractInput, submitButton)),
  modal("Account Settings", settingsId, accountSettingsForm(action:="/settings")),
  modal( "Transfer with fixed BoxId", boxTransactionId, form(action:="/send/withbox")(
    privateKeyInput, publicKeyLabel, boxIdInput, feeInput, amountInput, changeInput, addressInput, submitButton))
  )

  lazy val view: Text.TypedTag[String] = page(layout)(modals)

  lazy val layout: Text.TypedTag[String] = div(cls:="container-fluid")(
    div(cls:="row")(
      div(cls:="col-3")(
        h1("Encry Wallet"),
        div(cls:="btn-group-vertical")(
          modalButton(addressSendId)(addressSendId),
          modalButton(boxSendId)(boxSendId),
          modalButton(boxTransactionId)(boxTransactionId),
          button(tpe:="button", cls:="btn btn-outline-warning", attr("data-toggle"):="modal",
            attr("data-target"):=s"#$settingsId")("Account Settings"),
        ),
      ),
      div(cls:="col-9")(transactionHistory.view),
    )
  )

  def modalLink(modalId: String): Text.TypedTag[String] = a(attr("data-toggle"):="modal",
    attr("data-target"):=s"#$modalId")

  lazy val accountSettingsForm: Text.TypedTag[String] = form(
    div( cls:="form-group")(
      label(`for`:="privateKeyInput")(s"Private key: $secretKey"),
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
            button(tpe:="button", cls:="close", attr("data-dismiss"):="modal")(raw("&times;"))
          ),
          div(cls:="modal-body")(formBody),
        )
      )
    )

  def navBar: Text.TypedTag[String] =
    tag("nav")(cls:="navbar navbar-expand-lg navbar-light bg-light")(
      ul(cls:="nav nav-pills nav-fill")(
        li(cls:="nav-item")(a(cls:="nav-link disabled", style:="cursor:pointer")("WALLET")),
        li(cls:="nav-item")(modalLink(addressSendId)(cls:="nav-link", style:="cursor:pointer")("Simple transaction")),
        li(cls:="nav-item")(modalLink(boxSendId)(cls:="nav-link", style:="cursor:pointer")("Scripted transaction")),
        li(cls:="nav-item")(modalLink(boxTransactionId)(cls:="nav-link", style:="cursor:pointer")("Special transaction")),
        li(cls:="nav-item")(modalLink(settingsId)(cls:="nav-link", style:="cursor:pointer")("Settings")),
      )
    )

  def mainContainer(inner: Modifier*): Text.TypedTag[String] =
    div(cls:="container")(div(cls:="row")(div(cls:="col-12")(inner)))

  def alert = error.map(div(cls:="alert alert-warning", role:="alert")(_)).getOrElse(div())
  def alert2 = error.map(
    div(cls:="alert alert-warning alert-dismissible fade show fixed-bottom", role:="alert")(_)(
      button(tpe:="button",cls:="close", attr("data-dismiss"):="alert", attr("aria-label"):="Close")(raw("&times")))
  ).getOrElse(div())

  def view2: Text.TypedTag[String] = page(navBar, mainContainer(transactionHistory.view), modals, alert2)
}
