package com.playground.shoppingcart.endpoint

import cats._
import cats.effect.kernel.Async
import cats.syntax.all._
import com.playground.shoppingcart.checkout.Checkout
import com.playground.shoppingcart.domain.cart.CartService
import com.playground.shoppingcart.endpoint.validation.AuthValidation
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.dsl.request
import org.http4s.headers.Authorization
import org.http4s.implicits._
import tsec.mac.jca.HMACSHA256
import tsec.mac.jca.MacSigningKey

class OrderEndpoint[F[_]: Async](
  cartService: CartService[F],
  authValidation: AuthValidation[F],
) extends Http4sDsl[F] {

  def checkout: HttpRoutes[F] = HttpRoutes.of[F] { case request @ POST -> Root / "checkout" =>
    authValidation.asAuthUser { authRequet =>
      for {
        checkoutR <- request.as[Checkout]
        resp      <- Ok()
      } yield resp
    }(request)
  }

  private def validateCreditCard(str: String) = 
    str.forall(_.isDigit)

  def endpoints: HttpRoutes[F] = checkout

}

object OrderEndpoint {

  def endpoints[F[_]: Async](
    cartService: CartService[F],
    authValidation: AuthValidation[F],
  ): HttpRoutes[F] = new OrderEndpoint[F](cartService, authValidation).endpoints

}
