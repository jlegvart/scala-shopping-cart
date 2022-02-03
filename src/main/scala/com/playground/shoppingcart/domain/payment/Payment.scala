package com.playground.shoppingcart.domain.payment


final case class Payment(id: Option[Int] = None, payer: String, creditCard: String)