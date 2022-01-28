package com.playground.shoppingcart.repository

import com.playground.shoppingcart.domain.cart.Cart
import cats.effect._
import cats.syntax.all._
import cats.data.OptionT
import com.playground.shoppingcart.domain.cart.NewCartItem

class CartRepository[F[_]: Sync](store: Ref[F, Map[Int, Cart]]) {

  def cartByUser(userId: Int): F[Option[Cart]] =
    for {
      map      <- store.get
      userCart <- map.get(userId).pure[F]
    } yield userCart

  def updateCart(userId: Int, cart: Cart) = store.update {
    _ + (userId -> cart)
  }

}

object CartRepository {

  def apply[F[_]: Sync](store: Ref[F, Map[Int, Cart]]) = new CartRepository(store)

}
