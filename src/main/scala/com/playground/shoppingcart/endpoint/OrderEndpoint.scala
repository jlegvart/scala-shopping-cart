package com.playground.shoppingcart.endpoint

import cats.effect.kernel.Async
import com.playground.shoppingcart.domain.cart.CartService
import tsec.mac.jca.MacSigningKey
import tsec.mac.jca.HMACSHA256
import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes

class OrderEndpoint[F[_]: Async](
  cartService: CartService[F],
  key: MacSigningKey[HMACSHA256],
) extends Http4sDsl[F] {

  def checkout: HttpRoutes[F] = HttpRoutes.of[F] { case request @ POST -> Root / "checkout" => Ok() }

  def endpoints: HttpRoutes[F] = checkout

}

object OrderEndpoint {

  def endpoints[F[_]: Async](
    cartService: CartService[F],
    key: MacSigningKey[HMACSHA256],
  ): HttpRoutes[F] = new OrderEndpoint[F](cartService, key).endpoints

}
