package com.playground.shoppingcart.domain.cart

import cats._
import cats.syntax.all._
import cats.effect.Sync

import com.playground.shoppingcart.repository.CartRepository
import cats.data.OptionT
import com.playground.shoppingcart.domain.validation.CartUpdateFailed
import cats.data.EitherT

class CartService[F[_]: Sync](cartRepository: CartRepository[F]) {

  def getUserCart(userId: Int): F[Cart] = cartRepository.cartByUser(userId).map {
    case None       => Cart.empty
    case Some(cart) => cart
  }

  def updateCart(userId: Int, item: CartItem) =
    for {
      cart <- EitherT.liftF[F, CartUpdateFailed, Cart](getUserCart(userId))
      _    <- EitherT.fromEither(itemExistsInCart(item, cart))
      newCart = Cart(item :: cart.items, 0)
      _    <- EitherT.liftF[F, CartUpdateFailed, Unit](cartRepository.updateCart(userId, newCart))
    } yield ()

  def deleteCart(userId: Int): F[Unit] = cartRepository.deleteCart(userId)

  def itemExistsInCart(item: CartItem, cart: Cart): Either[CartUpdateFailed, Unit] = cart
    .items
    .find(_.itemId == item.itemId)
    .map(_ => CartUpdateFailed("Item already exists"))
    .toLeft(())

}
