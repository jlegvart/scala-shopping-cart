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
import tsec.authentication._
import tsec.jws.mac.JWTMac
import tsec.mac.jca.MacSigningKey
import tsec.mac.jca.HMACSHA256
import cats.data.Kleisli
import com.playground.shoppingcart.domain.user.User
import org.http4s.headers.Authorization
import cats.data.EitherT
import cats.effect.std
import com.playground.shoppingcart.domain.auth.JWTClaim
import com.playground.shoppingcart.domain.validation.UserAuthenticationError

class CartEndpoint[F[_]: Async: std.Console](
  cartService: CartService[F],
  key: MacSigningKey[HMACSHA256],
) extends Http4sDsl[F] {

  def getUserCart(): HttpRoutes[F] = HttpRoutes.of[F] { case request @ GET -> Root =>
    val resp =
      for {
        header <- EitherT.fromEither[F](
          request.headers.get[Authorization].toRight(new RuntimeException("Error"))
        )
        token = header.value.split(" ")(1)
        parsed <- JWTMac.verifyAndParse[F, HMACSHA256](token, key).attemptT
        claims <- EitherT.fromEither(parsed.body.getCustom[JWTClaim]("user"))
        _ <- EitherT.liftF(std.Console[F].println(claims.username))
        body <- EitherT.rightT[F, Throwable](parsed.body)
      } yield body

    resp.value.flatMap { a =>
      a match {
        case Left(x)  => Ok("no auth")
        case Right(b) => Ok("exists")
      }
    }
  }

  private def authUser(authHeader: String): Either[UserAuthenticationError, User] = ???

  private def endpoints: HttpRoutes[F] = getUserCart

}

object CartEndpoint {

  def endpoints[F[_]: Async: std.Console](
    cartService: CartService[F],
    key: MacSigningKey[HMACSHA256],
  ): HttpRoutes[F] = new CartEndpoint[F](cartService, key).endpoints

}
