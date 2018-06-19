import scalatags.Text
import scalatags.Text.all._

object Page {
  def accountForm = form()(
    div( cls:="form-group")(
      label(`for`:="publicKey")("Amount"),
      input(tpe:="number", cls:="form-control", id:="publicKey", placeholder:="0", name:="amount")
    ),
    div( cls:="form-group")(
      label(`for`:="exampleAddressInput")("Address"),
      input(tpe:="text", cls:="form-control", id:="exampleAddressInput", placeholder:="Enter address here",name:="address"),
    ),
    button(tpe:="submit", cls:="btn btn-primary")("Submit")
  )

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

  lazy val transactionFormInner = Seq(
    div( cls:="form-group")(
      label(`for`:="exampleCurrencyInput")("Private key:"),
      input(tpe:="text", cls:="form-control", id:="exampleCurrencyInput", placeholder:="ABCDEF", name:="prKey", disabled)
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
      input(tpe:="text", cls:="form-control", id:="exampleAddressInput", placeholder:="Enter address here",name:="recepient"),
    ),
  )

  val contractInput = div( cls:="form-group")(
    label(`for`:="exampleContractInput")("Contract"),
    textarea(tpe:="text", cls:="form-control", id:="exampleContractInput", placeholder:="ScriptCode",name:="recepient"),
  )

  lazy val submitButton = button(tpe:="submit", cls:="btn btn-primary")("Submit")

  lazy val transactionForm1 = form(transactionFormInner)(submitButton)
  lazy val transactionForm2 = form(transactionFormInner)(contractInput, submitButton)

  lazy val buttonGroup =
    div(cls:="btn-group-vertical")(
      modalButton("myModal")("Transaction1"),
      modalButton("myModal2")("Transaction2"),
      modalButton("myModal2")("Transaction3")
    )

  lazy val layout = div(cls:="container-fluid")(
    div(cls:="row")(
      div(cls:="col-3")(buttonGroup),
      div(cls:="col-9"),
    )
  )

  def modalButton(modalId: String) = button(tpe:="button", cls:="btn btn-primary", attr("data-toggle"):="modal",
    attr("data-target"):=s"#$modalId")
  lazy val jumbModalButton =
    div(cls:="jumbotron")(modalButton("SendMoney"))

  lazy val modalForm = form()(
    div( cls:="form-group")(
      label(`for`:="exampleCurrencyInput")("Private key:"),
      input(tpe:="text", cls:="form-control", id:="exampleCurrencyInput", placeholder:="ABCDEF", name:="amount", disabled)
    ),
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

  lazy val example = page(layout,modal,modal2)

  lazy val modal: Modifier =
    div(cls:="modal", id:="myModal")(
      div(cls:="modal-dialog")(
        div(cls:="modal-content")(
          div(cls:="modal-header")(
            h4(cls:="modal-title")("Transfer"),
            button(tpe:="button", cls:="close", attr("data-dissmiss"):="modal")(raw("&times;"))
          ),
          div(cls:="modal-body")(transactionForm1),
//          div(cls:="modal-footer")(
//            button(tpe:="button",cls:="btn btn-primary")("Save changes"),
//            button(tpe:="button",cls:="btn btn-secondary", attr("data-dissmiss"):="modal")("Close"),
//          )
        )
      )
    )

  def numberInput(inputName: String, idPrefix: String) = div( cls:="form-group")(
    label(`for`:=s"$idPrefix${inputName.capitalize}Input")(inputName.capitalize),
    input(tpe:="number", cls:="form-control", id:=s"$idPrefix${inputName.capitalize}Input", placeholder:="0",
      name :=s"${inputName.toLowerCase}")
  )

  lazy val modal2: Modifier =
    div(cls:="modal", id:="myModal2")(
      div(cls:="modal-dialog")(
        div(cls:="modal-content")(
          div(cls:="modal-header")(
            h4(cls:="modal-title")("Transfer"),
            button(tpe:="button", cls:="close", attr("data-dissmiss"):="modal")(raw("&times;"))
          ),
          div(cls:="modal-body")(transactionForm2),
          //          div(cls:="modal-footer")(
          //            button(tpe:="button",cls:="btn btn-primary")("Save changes"),
          //            button(tpe:="button",cls:="btn btn-secondary", attr("data-dissmiss"):="modal")("Close"),
          //          )
        )
      )
    )
}