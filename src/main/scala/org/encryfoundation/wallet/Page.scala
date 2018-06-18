package org.encryfoundation.wallet

import scalatags.Text.all._

object Page {
  val integrity: Attr = attr("integrity")
  val crossorigin = attr("crossorigin")
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

  val modalButton =
    div(cls:="jumbotron")(
      button(tpe:="button", cls:="btn btn-primary", attr("data-toggle"):="modal",
        attr("data-target"):="#myModal")(
        "Send money"
      )
  )

  val modalForm = form()(
    div( cls:="form-group")(
      label(`for`:="exampleCurrencyInput")("Amount"),
      input(tpe:="number", cls:="form-control", id:="exampleCurrencyInput", placeholder:="0", name:="amount")
    ),
    div( cls:="form-group")(
      label(`for`:="exampleAddressInput")("Address"),
      input(tpe:="text", cls:="form-control", id:="exampleAddressInput", placeholder:="Enter address here",name:="address"),
    ),
    button(tpe:="submit", cls:="btn btn-primary")("Submit")
  )

  val modal: Modifier =
    div(cls:="modal", id:="myModal")(
      div(cls:="modal-dialog")(
        div(cls:="modal-content")(
          div(cls:="modal-header")(
            h4(cls:="modal-title")("Transfer"),
            button(tpe:="button", cls:="close", attr("data-dissmiss"):="modal")(raw("&times;"))
          ),
          div(cls:="modal-body")(modalForm),
          div(cls:="modal-footer")(
            button(tpe:="button",cls:="btn btn-primary")("Save changes"),
            button(tpe:="button",cls:="btn btn-secondary", attr("data-dissmiss"):="modal")("Close"),
          )
        )
      )
    )
}