package org.encryfoundation.wallet

import scalatags.Text.all._

object Page {

  lazy val integrity: Attr = attr("integrity")
  lazy val crossorigin = attr("crossorigin")
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

  lazy val submitButton = button(tpe:="submit", cls:="btn btn-primary")("Submit")

  def modalButton(modalId: String) = button(tpe:="button", cls:="btn btn-outline-primary", attr("data-toggle"):="modal",
    attr("data-target"):=s"#$modalId")


  def numberInput(inputName: String, idPrefix: String) = div( cls:="form-group")(
    label(`for`:=s"$idPrefix${inputName.capitalize}Input")(inputName.capitalize),
    input(tpe:="number", cls:="form-control", id:=s"$idPrefix${inputName.capitalize}Input", placeholder:="0",
      name :=s"${inputName.toLowerCase}")
  )
}