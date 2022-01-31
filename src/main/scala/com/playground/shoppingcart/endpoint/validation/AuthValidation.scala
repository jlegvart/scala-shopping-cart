package com.playground.shoppingcart.endpoint.validation

import cats._
import cats.data.EitherT
import cats.data.OptionT
import cats.effect.kernel.Async
import cats.syntax.all._
import com.playground.shoppingcart.domain.auth.JWTClaim
import com.playground.shoppingcart.domain.user.Role
import com.playground.shoppingcart.domain.user.User
import com.playground.shoppingcart.domain.validation.UserAuthenticationError
import com.playground.shoppingcart.endpoint.AuthorizedRequest
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

class AuthValidation[F[_]: Async](key: MacSigningKey[HMACSHA256]) {

  def asAuthUser(
    f: AuthorizedRequest[F] => F[Response[F]]
  )(
    request: Request[F]
  ) = authUser(request).value.flatMap {
    case Left(value) => Response[F](status = Status.Unauthorized).pure[F]
    case Right(user) => f(AuthorizedRequest[F](request, user))
  }

  private def authUser(request: Request[F]): EitherT[F, UserAuthenticationError, User] =
    for {
      header <- EitherT.fromEither[F](
        request
          .headers
          .get[Authorization]
          .toRight(UserAuthenticationError("Invalid authorization header"))
      )
      tokenStr <- getToken(header.value).toRight(
        UserAuthenticationError("Missing authorization token")
      )
      parsedToken <- EitherT(parseJWTToken(tokenStr, key))
      user        <- EitherT.fromEither(userFromToken(parsedToken))
    } yield user

  private def getToken(authHeader: String): OptionT[F, String] = {
    val authToken = authHeader.split(" ")
    if (authToken.length != 2)
      OptionT.none
    else
      OptionT.pure(authToken(1))
  }

  private def parseJWTToken(
    token: String,
    key: MacSigningKey[HMACSHA256],
  ): F[Either[UserAuthenticationError, JWTMac[HMACSHA256]]] = JWTMac
    .verifyAndParse[F, HMACSHA256](token, key)
    .flatMap(parsedToken => parsedToken.asRight[UserAuthenticationError].pure[F])
    .handleError(_ => UserAuthenticationError("Invalid token").asLeft)

  private def userFromToken(token: JWTMac[HMACSHA256]): Either[UserAuthenticationError, User] =
    token
      .body
      .getCustom[JWTClaim]("user")
      .fold(
        _ =>
          UserAuthenticationError("Invalid JWT token structure, cannot extract user data")
            .asLeft[User],
        claims => User(Some(claims.userId), claims.username, "", Role.toRole(claims.role)).asRight,
      )

}
