package com.playground.shoppingcart.domain.auth

object AuthRequest {
    final case class RegisterRequest(username: String, password: String)
    final case class LoginRequest(username: String, password: String)
}
