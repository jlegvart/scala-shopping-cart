package com.playground.shoppingcart.repository

import com.playground.shoppingcart.domain.cart.Cart
import cats.effect._
import cats.implicits._

class CartRepository[F[_]: Sync](store: Ref[F, Map[Int, Cart]]) {

  def cartByUser(userId: Int) =
    for {
      map      <- store.get
      userCart <- map.get(userId).pure[F]
    } yield userCart

}

object CartRepository {

  // def apply[F[_]](store: Ref[F, Map[Int, Cart]]) = new CartRepository(store)

}
