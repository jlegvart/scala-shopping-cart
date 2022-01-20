package com.playground.shoppingcart.domain.cart

import com.playground.shoppingcart.repository.CartRepository

class CartService[F[_]](cartRepository: CartRepository[F]) {
  
    def getUserCart(userId: Int): F[Option[Cart]] = 
        cartRepository.cartByUser(userId)

    def updateCart(cart: Cart): F[Option[Cart]] = 
        cartRepository.updateCart(cart)

    def deleteCart(userId: Int): F[Unit] = 
        cartRepository.deleteCart(userId)
}
