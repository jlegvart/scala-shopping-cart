package com.playground.shoppingcart.domain.order

import cats.effect.Sync
import cats.syntax.all._
import com.playground.shoppingcart.domain.payment.Payment
import com.playground.shoppingcart.domain.payment.PaymentService
import com.playground.shoppingcart.repository.OrderRepository
import doobie._
import doobie.hikari.HikariTransactor
import doobie.implicits._

class OrderService[F[_]: Sync](
  paymentService: PaymentService[F],
  orderRepository: OrderRepository[F],
  transactor: HikariTransactor[F],
) {

  def create(order: Order, payment: Payment): F[Order] = {
    val q =
      for {
        newPayment <- paymentService.create(payment)
        updOrder = order.copy(paymentId = Some(newPayment.id.get))
        newOrder <- orderRepository.create(updOrder)
      } yield newOrder

    q.transact(transactor)
  }

}
