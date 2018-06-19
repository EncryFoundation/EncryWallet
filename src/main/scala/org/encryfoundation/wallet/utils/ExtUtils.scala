package org.encryfoundation.wallet.utils

object ExtUtils {
  trait Printer[U]{
    def print[T](obj: T): Unit
  }

  implicit class Appliable[T](val obj: T) extends AnyVal {
    def rapply[S](f: T => S ): S = { f(obj)}
    def iapply[S](f: T => S ): T = { f(obj); obj}
  }

  implicit class Traceable[T](val obj: T) extends AnyVal {
    def trace: T = { println(obj); obj}
    def trace[U]( u: U): T = { println(u); obj}
    def traceWith[S](reader: T => S ): T = { println(reader(obj)); obj}
  }
}
