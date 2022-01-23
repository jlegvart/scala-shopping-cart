package com.playground.shoppingcart.domain.cart

import com.playground.shoppingcart.domain.item.Item
import com.playground.shoppingcart.domain.user.User
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

final case class Cart(items: List[CartItem])
final case class CartItem(itemId: Int, itemName: String, quantity: Int)

object Cart {

  implicit val cartEncoder: Encoder[Cart] = deriveEncoder[Cart]
  implicit val cartDecoder: Decoder[Cart] = deriveDecoder[Cart]

  implicit val cartItemEncoder: Encoder[CartItem] = deriveEncoder[CartItem]
  implicit val cartItemDecoder: Decoder[CartItem] = deriveDecoder[CartItem]

  def empty: Cart = Cart(List.empty)

}