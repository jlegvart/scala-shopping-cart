package com.playground.shoppingcart.repository

import doobie.hikari.HikariTransactor
import com.playground.shoppingcart.domain.order.Order
import cats._
import cats.data._
import cats.effect._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import com.playground.shoppingcart.domain.order.OrderItem

class OrderRepository[F[_]: Sync](transactor: HikariTransactor[F]) {

  def create(order: Order): ConnectionIO[Order] = {
    val orderQ =
      sql"INSERT INTO user_order (payment_id, status, price) VALUES (${order.paymentId}, ${order.status.toString()}, ${order.price})"
        .update
        .withUniqueGeneratedKeys[Int]("id")
        .map(id => order.copy(id = Some(id)))

    for {
      newOrder <- orderQ
      orderItems <- newOrder
        .items
        .traverse(orderItem => createOrderItem(newOrder.id.get, orderItem))
    } yield newOrder.copy(items = orderItems)
  }

  private def createOrderItem(orderId: Int, orderItem: OrderItem): ConnectionIO[OrderItem] =
    sql"INSERT INTO user_order_item (order_id, item_id, quantity, price) VALUES ($orderId, ${orderItem.itemId}, ${orderItem.quantity}, ${orderItem.price})"
      .update
      .withUniqueGeneratedKeys[Int]("id")
      .map(id => orderItem.copy(id = Some(id)))

}
