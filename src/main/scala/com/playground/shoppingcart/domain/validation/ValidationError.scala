package com.playground.shoppingcart.domain.validation

trait ValidationError extends Throwable {
    def msg: String
}
case class UserAuthenticationError(msg: String) extends ValidationError
