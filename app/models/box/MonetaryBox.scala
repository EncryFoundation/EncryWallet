package models.box

import models.box.Box.Amount

trait MonetaryBox extends EncryBaseBox {

  val amount: Amount
}
