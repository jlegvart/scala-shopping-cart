package com.playground.shoppingcart.domain.cart

import com.playground.shoppingcart.repository.CartRepository

class CartService[F[_]](cartRepository: CartRepository[F]) {
  
    def getUserCart(userId: Int): F[Cart] = ???

}
