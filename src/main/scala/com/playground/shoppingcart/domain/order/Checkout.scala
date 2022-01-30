package com.playground.shoppingcart.checkout

final case class Checkout(payer: String, creditCard: String, exp: String, ccv: Int)