package com.playground.shoppingcart.endpoint

import cats.effect.kernel.Async
import com.playground.shoppingcart.domain.cart.CartService
import org.http4s.HttpRoutes
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.implicits._
import cats._
import cats.syntax.all._

class CartEndpoint[F[_]: Async](cartService: CartService[F]) extends Http4sDsl[F] {

  def getUserCart(): HttpRoutes[F] = HttpRoutes.of[F] { 
    case GET -> Root => 
        Ok("cart")
}

  private def endpoints: HttpRoutes[F] = getUserCart

}

object CartEndpoint {

  def endpoints[F[_]: Async](cartService: CartService[F]): HttpRoutes[F] =
    new CartEndpoint[F](cartService).endpoints

}
