package models.box

import models.box.Box.Amount

trait EncryBox[P <: EncryProposition] extends EncryBaseBox {

  override val proposition: P

  // Shadow redundant field from base class.
  override val value: Amount = 0L
}

object EncryBox {

  type BxTypeId = Byte

  val BoxIdSize = 32
}
