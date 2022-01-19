package com.playground.shoppingcart.repository

import doobie._
import doobie.implicits._
import cats._
import cats.effect._
import cats.implicits._
import com.playground.shoppingcart.domain.item.Item

class ItemRepository[F[_]: MonadCancelThrow](xa: Transactor[F]) {

  def getAll(): F[List[Item]] =
    sql"""
        SELECT item.id, company.id, company.name, 
            category.id, category.name, item.name, item.price 
        FROM item 
        LEFT JOIN company ON item.company_id = company.id
        LEFT JOIN category ON item.category_id = category.id
        """
      .query[Item]
      .to[List]
      .transact(xa)
}
