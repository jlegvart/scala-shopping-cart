package com.playground.shoppingcart.repository

import cats.effect._
import cats.implicits._
import com.playground.shoppingcart.domain.item.Item
import doobie._
import doobie.implicits._

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

  def itemById(itemId: Int): F[Option[Item]] =
    sql"""
        SELECT item.id, company.id, company.name, 
            category.id, category.name, item.name, item.price 
        FROM item 
          LEFT JOIN company ON item.company_id = company.id
          LEFT JOIN category ON item.category_id = category.id
        WHERE item.id = $itemId
        """
      .query[Item]
      .option
      .transact(xa)

}
