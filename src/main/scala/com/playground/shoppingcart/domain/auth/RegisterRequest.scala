package com.playground.shoppingcart.domain.auth

import io.circe.generic.auto._

final case class RegisterRequest(username: String, password: String)
