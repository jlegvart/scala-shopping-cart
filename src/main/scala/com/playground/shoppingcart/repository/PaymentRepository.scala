package com.playground.shoppingcart.repository

import doobie.hikari.HikariTransactor
import com.playground.shoppingcart.domain.payment.Payment

class PaymentRepository[F[_]](transactor: HikariTransactor[F]) {
  

    def createNewPayment(payment: Payment) = ???

}
