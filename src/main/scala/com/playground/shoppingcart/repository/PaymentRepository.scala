package com.playground.shoppingcart.repository

import doobie.hikari.HikariTransactor
import com.playground.shoppingcart.domain.payment.Payment
import cats._
import cats.data._
import cats.effect._
import cats.syntax.all._
import doobie._
import doobie.implicits._

class PaymentRepository[F[_]](transactor: HikariTransactor[F]) {

  def create(payment: Payment): ConnectionIO[Payment] =
    sql"INSERT INTO payment (payer, credit_card) VALUES (${payment.payer}, ${payment.creditCard})"
      .update
      .withUniqueGeneratedKeys[Int]("id")
      .map(id => payment.copy(id = Some(id)))

}
