package com.playground.shoppingcart.domain.order

import com.playground.shoppingcart.domain.item.Item

final case class Order(
  id: Option[Int] = None,
  paymentId: Option[Int],
  userId: Int,
  items: List[OrderItem],
  price: BigDecimal,
)

final case class OrderItem(id: Option[Int], itemId: Int, quantity: Int, price: BigDecimal)
