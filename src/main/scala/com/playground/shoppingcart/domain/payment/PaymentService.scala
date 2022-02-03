package com.playground.shoppingcart.domain.payment

import com.playground.shoppingcart.repository.PaymentRepository
import doobie._

class PaymentService[F[_]](paymentRepository: PaymentRepository[F]) {

  def create(payer: String, creditCard: String): ConnectionIO[Payment] = paymentRepository.create(
    Payment(None, payer, creditCard)
  )

}
