package com.playground.shoppingcart.repository

import doobie.hikari.HikariTransactor

final case class AuthRepository[F[_]](transactor: HikariTransactor[F]) {



}