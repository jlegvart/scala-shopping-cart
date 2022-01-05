package com.playground.shoppingcart.endpoint

import org.http4s.HttpRoutes
import cats.effect._
import cats.syntax.all._
import cats.data._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import com.playground.shoppingcart.domain.user.UserService

class AuthEndpoint[F[_]: Sync](userService: UserService[F]) extends Http4sDsl[F] {

  private def loginEndpoint: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    Ok("login")
  }

  private def registerEndpoint: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "register" =>
    Ok("register")
  }

  def endpoints: HttpRoutes[F] = loginEndpoint <+> registerEndpoint
}

object AuthEndpoint {
  def endpoints[F[_]: Sync](userService: UserService[F]) =
    new AuthEndpoint[F](userService).endpoints
}