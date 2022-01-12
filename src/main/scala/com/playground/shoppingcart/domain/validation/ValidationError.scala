package com.playground.shoppingcart.domain.validation

trait ValidationError
case class UserAuthenticationError() extends ValidationError
