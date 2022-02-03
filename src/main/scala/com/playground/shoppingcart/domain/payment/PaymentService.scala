package com.playground.shoppingcart.domain.payment

import cats._
import cats.syntax.all._
import com.playground.shoppingcart.repository.PaymentRepository
import doobie._
import cats.effect.kernel.Async
import cats.effect.kernel.Sync

class PaymentService[F[_]: Sync](paymentRepository: PaymentRepository[F]) {

  def executePayment(
    payer: String,
    creditCard: String,
  ): F[Payment] = Payment(None, payer, creditCard).pure[F]

  def create(payment: Payment): ConnectionIO[Payment] = paymentRepository.create(payment)

}
