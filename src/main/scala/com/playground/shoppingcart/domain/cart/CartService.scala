package com.playground.shoppingcart.domain.cart

import cats.data.EitherT
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.all._
import com.playground.shoppingcart.domain.item.Item
import com.playground.shoppingcart.domain.validation.CartUpdateError
import com.playground.shoppingcart.repository.CartRepository

class CartService[F[_]: Sync](cartRepository: CartRepository[F]) {

  def getUserCart(userId: Int): F[Cart] = cartRepository.cartByUser(userId).map {
    case None       => Cart.empty
    case Some(cart) => cart
  }

  def updateCart(userId: Int, item: CartItem): F[Either[CartUpdateError, Unit]] =
    (for {
      cart <- EitherT.liftF[F, CartUpdateError, Cart](getUserCart(userId))
      _    <- EitherT.fromEither(itemExistsInCart(item, cart))
      items   = item :: cart.items
      newCart = Cart(items, calculateCheckoutPrice(items))
      _ <- EitherT.liftF[F, CartUpdateError, Unit](cartRepository.updateCart(userId, newCart))
    } yield ()).value

  def deleteCart(userId: Int): F[Unit] = cartRepository.deleteCart(userId)

  def itemExistsInCart(cartItem: CartItem, cart: Cart): Either[CartUpdateError, Unit] = cart
    .items
    .find(_.item.id == cartItem.item.id)
    .map(_ => CartUpdateError("Item already exists"))
    .toLeft(())

  def calculateCheckoutPrice(cartItems: List[CartItem]): BigDecimal =
    cartItems.map(itemQuantTotal).sum

  def itemQuantTotal: CartItem => BigDecimal = cartItem => cartItem.quantity * cartItem.item.price
}
