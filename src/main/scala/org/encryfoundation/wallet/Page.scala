package org.encryfoundation.wallet

import scalatags.Text
import scalatags.Text.all._
trait Page {

  lazy val integrity: Attr = attr("integrity")
  lazy val crossorigin: Attr = attr("crossorigin")
  def page(el: Modifier*) = html(
    head(
      script(src:="https://code.jquery.com/jquery-3.2.1.slim.min.js",
        integrity:="sha384-KJ3o2DKtIkvYIK3UENzmM7KCkRr/rE9/Qpg6aAZGJwFDMVNA/GpGFF93hXpG5KkN",
        crossorigin:="anonymous"),
      script(src:="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js",
        integrity:="sha384-ApNbgh9B+Y1QKtv3Rn7W3mgPxhU9K/ScQsAP7hUibX39j7fakFPskvXusvfa0b4Q",
        crossorigin:="anonymous"),
      script(src:="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js",
        integrity:="sha384-JZR6Spejh4U02d8jOt6vLEHfe/JQGiRRSQQxSfFWpi1MquVdAyjUar5+76PVCmYl",
        crossorigin:="anonymous"),
      link(rel:="stylesheet", href:="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css",
        integrity:="sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm",
        crossorigin:="anonymous")
    ),
    body(el)
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

  def modal(title: String, modalId: String, formBody: Modifier): Modifier =
    div(cls:="modal", id:=modalId)(
      div(cls:="modal-dialog")(
        div(cls:="modal-content")(
          div(cls:="modal-header")(
            h4(cls:="modal-title")(title),
            button(tpe:="button", cls:="close", attr("data-dismiss"):="modal")(raw("&times;"))
          ),
          div(cls:="modal-body")(formBody)
        )
      )
    )

  val addressSendId: String = "SendToAddress"
  val boxSendId: String = "SendWithContract"
  val boxTransactionId: String = "SendWithBox"
  val settingsId: String = "accountSettings"

  def modalLink(modalId: String): Text.TypedTag[String] = a(attr("data-toggle"):="modal", attr("data-target"):=s"#$modalId")

  def navBar: Text.TypedTag[String] =
    tag("nav")(cls:="navbar navbar-expand-lg navbar-light bg-light")(
      ul(cls:="nav nav-pills nav-fill")(
        li(cls:="nav-item")(a(cls:="nav-link disabled", style:="cursor:pointer")(h3("WALLET"))),
        li(cls:="nav-item")(modalLink(addressSendId)(cls:="nav-link", style:="cursor:pointer")("Simple transaction")),
        li(cls:="nav-item")(modalLink(boxSendId)(cls:="nav-link", style:="cursor:pointer")("Scripted transaction")),
        li(cls:="nav-item")(modalLink(boxTransactionId)(cls:="nav-link", style:="cursor:pointer")("Special transaction")),
        li(cls:="nav-item")(modalLink(settingsId)(cls:="nav-link", style:="cursor:pointer")("Settings"))
      )
    )

  lazy val submitButton: Text.TypedTag[String] = button(tpe:="submit", cls:="btn btn-primary")("Submit")
}
