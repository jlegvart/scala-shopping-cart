package com.playground.shoppingcart.domain.validation

trait ValidationError extends Throwable {
  def msg: String
}

case class UserAuthenticationError(msg: String) extends ValidationError
case class CartUpdateError(msg: String)         extends ValidationError
case class CheckoutError(msg: String)           extends ValidationError
