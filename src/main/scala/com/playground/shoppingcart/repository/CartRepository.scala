package com.playground.shoppingcart.repository

import com.playground.shoppingcart.domain.cart.Cart
import cats.effect._
import cats.syntax.all._
import cats.data.OptionT

class CartRepository[F[_]: Sync](store: Ref[F, Map[Int, Cart]]) {

  def cartByUser(userId: Int): F[Option[Cart]] =
    for {
      map      <- store.get
      userCart <- map.get(userId).pure[F]
    } yield userCart

  def updateCart(cart: Cart) =
    (for {
      userId <- OptionT.fromOption(cart.user.id)
      _ <- OptionT.liftF(store.update { m =>
        m + (userId -> cart)
      })
    } yield cart).value

  def deleteCart(userId: Int): F[Unit] = store.getAndUpdate(_ - userId) >> Sync[F].unit

}

object CartRepository {

  def apply[F[_]: Sync](store: Ref[F, Map[Int, Cart]]) = new CartRepository(store)

}
