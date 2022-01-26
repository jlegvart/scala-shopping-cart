package com.playground.shoppingcart.endpoint

import cats._
import cats.data.EitherT
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.kernel.Async
import cats.effect.std
import cats.syntax.all._
import com.playground.shoppingcart.domain.auth.JWTClaim
import com.playground.shoppingcart.domain.cart.Cart
import com.playground.shoppingcart.domain.cart.CartItem
import com.playground.shoppingcart.domain.cart.CartService
import com.playground.shoppingcart.domain.item.Item
import com.playground.shoppingcart.domain.user.Customer
import com.playground.shoppingcart.domain.user.Role
import com.playground.shoppingcart.domain.user.User
import com.playground.shoppingcart.domain.validation.UserAuthenticationError
import io.circe._
import io.circe.generic.auto
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import org.http4s.client.oauth1.HmacSha256
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.implicits._
import tsec.authentication._
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256
import tsec.mac.jca.MacSigningKey
import com.playground.shoppingcart.domain.item.ItemService
import com.playground.shoppingcart.domain.validation.CartUpdateError
import doobie.util.update

class CartEndpoint[F[_]: Async: std.Console](
  cartService: CartService[F],
  itemService: ItemService[F],
  key: MacSigningKey[HMACSHA256],
) extends Http4sDsl[F] {

  def getUserCart: HttpRoutes[F] = HttpRoutes.of[F] { case request @ GET -> Root =>
    authUser(request).value.flatMap {
      case Left(value) => Response[F](status = Status.Unauthorized).pure[F]
      case Right(user) =>
        cartService
          .getUserCart(user.id.get)
          .flatMap(cart => Ok(cart.asJson))
    }
  }

  def addToCart: HttpRoutes[F] = HttpRoutes.of[F] { case request @ POST -> Root =>
    authUser(request)
      .value
      .flatMap {
        case Left(value) => Response[F](status = Status.Unauthorized).pure[F]
        case Right(user) =>
          val action =
            for {
              cartItem <- EitherT.liftF[F, CartUpdateError, CartItem](request.as[CartItem])
              item <- EitherT.fromOptionF[F, CartUpdateError, Item](
                itemService.getItemById(cartItem.itemId),
                CartUpdateError("Wrong item id"),
              )
              _ <- cartService.updateCart(user.id.get, cartItem)
            } yield ()

          action.value.flatMap {
            case Left(updateFailedError) => BadRequest(updateFailedError.msg)
            case Right(_)                => Created()
          }
      }
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

  private def endpoints: HttpRoutes[F] = getUserCart <+> addToCart

}

object CartEndpoint {

  def endpoints[F[_]: Async: std.Console](
    cartService: CartService[F],
    itemService: ItemService[F],
    key: MacSigningKey[HMACSHA256],
  ): HttpRoutes[F] = new CartEndpoint[F](cartService, itemService, key).endpoints

}
