package com.playground.shoppingcart.repository

import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import com.playground.shoppingcart.domain.user.User

final case class UserRepository[F[_]: MonadCancelThrow](xa: Transactor[F]) {

  def getUserByUsername(username: String): F[Option[User]] =
    sql"SELECT id, username, password, role FROM users WHERE username = $username"
      .query[User]
      .option
      .transact(xa)

  def insertUser(user: User): F[Int] =
    sql"INSERT INTO users (username, password, role) VALUES  (${user.username}, ${user.password}, ${user.role})"
      .update
      .withUniqueGeneratedKeys[Int]("id")
      .transact(xa)

}
