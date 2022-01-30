package com.playground.shoppingcart.domain.order


final case class Payment(id: Option[Int] = None, payer: String, creditCard: String)