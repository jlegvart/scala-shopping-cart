package com.playground.shoppingcart.repository

import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.util.transactor.Transactor
import com.playground.shoppingcart.domain.category.Category

class CategoryRepository[F[_]: MonadCancelThrow](xa: Transactor[F]) {

  def getAllCategories(): F[List[Category]] = sql"SELECT id, name FROM category"
    .query[Category]
    .to[List]
    .transact(xa)

}
