package com.playground.shoppingcart.domain.order

import cats.Id
import com.playground.shoppingcart.domain.item.Item

trait OrderStatus
object Paid extends OrderStatus

final case class Order(
  id: Option[Int] = None,
  paymentId: Option[Int],
  status: OrderStatus,
  items: List[OrderItem],
  price: BigDecimal,
)

final case class OrderItem(id: Option[Int], itemId: Int, quantity: Int, price: BigDecimal)
