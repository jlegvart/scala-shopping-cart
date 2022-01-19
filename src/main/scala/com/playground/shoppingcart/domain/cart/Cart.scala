package com.playground.shoppingcart.domain.cart

import com.playground.shoppingcart.domain.item.Item
import com.playground.shoppingcart.domain.user.User

final case class Cart(user: User, items: List[CartItem])
final case class CartItem(item: Item, quantity: Int)