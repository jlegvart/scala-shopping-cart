package com.playground.shoppingcart.endpoint

import org.http4s.HttpRoutes
import cats.effect._
import cats.syntax.all._
import cats.data._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import com.playground.shoppingcart.domain.user.UserService
import com.playground.shoppingcart.domain.auth.RegisterRequest
import com.playground.shoppingcart.domain.user.User
import com.playground.shoppingcart.domain.user.Customer

class AuthEndpoint[F[_]: Async](userService: UserService[F]) extends Http4sDsl[F] {

  implicit val userDecoder = jsonOf[F, RegisterRequest]

  private def loginEndpoint: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    Ok("login")
  }

  private def registerEndpoint: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "register" =>
      for {
        reg <- req.as[RegisterRequest]
        user <- userService.getUser(reg.username)
        resp <-
          user match {
            case None =>
              userService
                .createUser(User(None, reg.username, reg.password, Customer))
                .flatMap(_ => Created())
            case Some(user) => BadRequest("User already exists")
          }
      } yield resp
  }

  def endpoints: HttpRoutes[F] = loginEndpoint <+> registerEndpoint
}

object AuthEndpoint {
  def endpoints[F[_]: Async](userService: UserService[F]) =
    new AuthEndpoint[F](userService).endpoints
}
