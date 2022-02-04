package com.playground.shoppingcart.endpoint

import cats.data._
import cats.effect._
import cats.syntax.all._
import com.playground.shoppingcart.domain.auth.AuthRequest.LoginRequest
import com.playground.shoppingcart.domain.auth.AuthRequest.RegisterRequest
import com.playground.shoppingcart.domain.auth.JWTClaim
import com.playground.shoppingcart.domain.user.Customer
import com.playground.shoppingcart.domain.user.User
import com.playground.shoppingcart.domain.user.UserService
import com.playground.shoppingcart.domain.validation.UserAuthenticationError
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.implicits._
import tsec.common.VerificationFailed
import tsec.common.Verified
import tsec.jws.mac._
import tsec.jwt._
import tsec.mac.jca.HMACSHA256
import tsec.mac.jca.MacSigningKey
import tsec.passwordhashers._
import tsec.passwordhashers.jca._

import scala.concurrent.duration._

class AuthEndpoint[F[_]](
  userService: UserService[F],
  key: MacSigningKey[HMACSHA256],
)(
  implicit F: Async[F]
) extends Http4sDsl[F] {

  implicit val userDecoder = jsonOf[F, RegisterRequest]
  implicit val logiDecoder = jsonOf[F, LoginRequest]

  private def loginEndpoint: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    val userVerification =
      for {
        loginRequest <- req.as[LoginRequest]
        user         <- userOrError(loginRequest.username)
        verified     <- passwordVerified(loginRequest.password, user)
      } yield user

    userVerification
      .flatMap(buildJWT)
      .flatMap(Ok(_))
      .handleErrorWith {
        case UserAuthenticationError(_) => BadRequest("Invalid username or password")
        case _: MessageFailure          => BadRequest()
        case _                          => InternalServerError()
      }
  }

  private def userOrError(username: String): F[User] = userService.getUser(username).flatMap {
    case Some(user) => F.pure(user)
    case None       => F.raiseError(UserAuthenticationError("Invalid username"))
  }

  private def passwordVerified(passwordReq: String, user: User): F[Unit] =
    for {
      verification <- BCrypt.checkpw[F](passwordReq.getBytes(), PasswordHash(user.password))
      verified <-
        if (verification == Verified)
          F.unit
        else
          F.raiseError(UserAuthenticationError("Invalid password"))
    } yield verified

  private def buildJWT(user: User): F[String] =
    for {
      claim <- JWTClaim(user.id.get, user.username, user.role.name).pure[F]
      claims <- JWTClaims.withDuration[F](
        customFields = Seq("user" -> claim.asJson),
        expiration = Some(10.minutes),
      )
      jwt <- JWTMac.buildToString[F, HMACSHA256](claims, key)
    } yield jwt

  private def registerEndpoint: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "register" =>
      for {
        reg  <- req.as[RegisterRequest]
        user <- userService.getUser(reg.username)
        pass <- BCrypt.hashpw[F](reg.password.getBytes())
        resp <-
          user match {
            case None =>
              userService
                .createUser(User(None, reg.username, pass, Customer))
                .flatMap(_ => Created())
            case Some(user) => BadRequest("User already exists")
          }
      } yield resp
  }

  def endpoints: HttpRoutes[F] = loginEndpoint <+> registerEndpoint
}

object AuthEndpoint {
  def endpoints[F[_]: Async](userService: UserService[F], key: MacSigningKey[HMACSHA256]) =
    new AuthEndpoint[F](userService, key).endpoints
}
