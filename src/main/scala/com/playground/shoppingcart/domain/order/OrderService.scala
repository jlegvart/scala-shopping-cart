package com.playground.shoppingcart.domain.order

import com.playground.shoppingcart.repository.OrderRepository
import doobie._

class OrderService[F[_]](orderRepository: OrderRepository[F]) {

  def create(order: Order): ConnectionIO[Order] = orderRepository.create(order)

}
