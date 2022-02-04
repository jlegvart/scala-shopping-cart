package com.playground.shoppingcart.endpoint.validation

import cats._
import cats.effect.kernel.Async
import cats.syntax.all._
import com.playground.shoppingcart.domain.auth.JWTClaim
import com.playground.shoppingcart.domain.user.Role
import com.playground.shoppingcart.domain.user.User
import com.playground.shoppingcart.domain.validation.UserAuthenticationError
import com.playground.shoppingcart.endpoint.AuthorizedRequest
import com.playground.shoppingcart.endpoint.AuthorizedUser
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.implicits._
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256
import tsec.mac.jca.MacSigningKey

class AuthValidation[F[_]](key: MacSigningKey[HMACSHA256])(implicit F: Async[F]) {

  def asAuthUser(
    f: AuthorizedRequest[F] => F[Response[F]]
  )(
    request: Request[F]
  ): F[Response[F]] = authUser(request)
    .flatMap { user =>
      f(AuthorizedRequest(request, new AuthorizedUser(user.id.get, user.username, user.role)))
    }
    .handleError {
      case UserAuthenticationError(msg) => Response(status = Status.Unauthorized)
      case _                            => Response(status = Status.InternalServerError)
    }

  private def authUser(request: Request[F]): F[User] =
    for {
      header      <- authHeader(request)
      token       <- getToken(header.value)
      parsedToken <- parseJWTToken(token, key)
      user        <- userFromToken(parsedToken)
    } yield user

  private def authHeader(request: Request[F]): F[Authorization] =
    request.headers.get[Authorization] match {
      case Some(header) => F.pure(header)
      case None         => F.raiseError(UserAuthenticationError("Missing auth header"))
    }

  private def getToken(authHeader: String): F[String] = {
    val authToken = authHeader.split(" ")
    if (authToken.length != 2)
      F.raiseError(UserAuthenticationError("Invalid token value"))
    else
      F.pure(authToken(1))
  }

  private def parseJWTToken(
    token: String,
    key: MacSigningKey[HMACSHA256],
  ): F[JWTMac[HMACSHA256]] = JWTMac
    .verifyAndParse[F, HMACSHA256](token, key)
    .handleErrorWith(_ => F.raiseError(UserAuthenticationError("Invalid token")))

  private def userFromToken(
    token: JWTMac[HMACSHA256]
  ): F[User] = F.pure(token.body.getCustom[JWTClaim]("user")).flatMap {
    case Left(failure) =>
      F.raiseError(UserAuthenticationError("Invalid JWT token structure, cannot extract user data"))
    case Right(claims) =>
      F.pure(User(Some(claims.userId), claims.username, "", Role.toRole(claims.role)))
  }

}
