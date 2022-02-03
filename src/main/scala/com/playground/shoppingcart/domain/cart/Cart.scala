package com.playground.shoppingcart.domain.cart

import com.playground.shoppingcart.domain.item.Item
import com.playground.shoppingcart.domain.user.User

final case class Cart(items: List[CartItem], total: BigDecimal)
final case class CartItem(item: Item, quantity: Int)
final case class NewCartItem(itemId: Int, quantity: Int)
final case class UpdateCartItems(items: Set[NewCartItem])

object Cart {
  def empty: Cart = Cart(List.empty, 0)

  def isEmpty(cart: Cart): Boolean = cart.items.isEmpty

}
