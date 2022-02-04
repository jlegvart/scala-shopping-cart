package com.playground.shoppingcart.endpoint

import cats._
import cats.data.EitherT
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.kernel.Async
import cats.effect.std
import cats.syntax.all._
import com.playground.shoppingcart.domain.cart.Cart
import com.playground.shoppingcart.domain.cart.CartItem
import com.playground.shoppingcart.domain.cart.CartService
import com.playground.shoppingcart.domain.cart.NewCartItem
import com.playground.shoppingcart.domain.cart.UpdateCartItems
import com.playground.shoppingcart.domain.item.Item
import com.playground.shoppingcart.domain.item.ItemService
import com.playground.shoppingcart.domain.user.Customer
import com.playground.shoppingcart.domain.user.Role
import com.playground.shoppingcart.domain.user.User
import com.playground.shoppingcart.domain.validation.CartUpdateError
import com.playground.shoppingcart.domain.validation.UserAuthenticationError
import com.playground.shoppingcart.endpoint.validation.AuthValidation
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import org.http4s.client.oauth1.HmacSha256
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.dsl.request
import org.http4s.headers.Authorization
import org.http4s.implicits._

class CartEndpoint[F[_]: Async](
  cartService: CartService[F],
  itemService: ItemService[F],
  authValidation: AuthValidation[F],
) extends Http4sDsl[F] {

  def getUserCart: HttpRoutes[F] = HttpRoutes.of[F] { case request @ GET -> Root =>
    authValidation.asAuthUser { authRequest =>
      cartService
        .getUserCart(authRequest.authUser.id)
        .flatMap(cart => Ok(cart.asJson))
    }(request)
  }

  def addToCart: HttpRoutes[F] = HttpRoutes.of[F] { case request @ POST -> Root =>
    authValidation.asAuthUser { authRequest =>
      val action =
        for {
          newItem <- EitherT.liftF[F, CartUpdateError, NewCartItem](request.as[NewCartItem])
          _       <- validateQuantity(newItem)
          item <- EitherT.fromOptionF[F, CartUpdateError, Item](
            itemService.getItemById(newItem.itemId),
            CartUpdateError("Invalid item id"),
          )
          user = authRequest.authUser
          _ <- EitherT(cartService.addToCart(user.id, CartItem(item, newItem.quantity)))
        } yield ()

      action.value.flatMap {
        case Left(updateFailedError) => BadRequest(updateFailedError.msg)
        case Right(_)                => Created()
      }
    }(request)
  }

  def updateCart: HttpRoutes[F] = HttpRoutes.of[F] { case request @ PUT -> Root =>
    authValidation.asAuthUser { authRequest =>
      val cartItems =
        for {
          updateCartReq <- request.as[UpdateCartItems]
          validatedCartItems = updateCartReq.items.map(asValidCartItem).toList
          items <- validatedCartItems.sequence
        } yield items.sequence

      cartItems.flatMap {
        case Left(updateError) => BadRequest(updateError.msg)
        case Right(items) =>
          cartService.updateCartItems(
            authRequest.authUser.id,
            items,
          ) >> NoContent()
      }
    }(request)
  }

  def deleteFromCart: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ DELETE -> Root / IntVar(itemId) =>
      authValidation.asAuthUser { authRequest =>
        cartService.deleteItemFromCart(authRequest.authUser.id, itemId) >>
          NoContent()
      }(request)
  }

  private def asValidCartItem: NewCartItem => F[Either[CartUpdateError, CartItem]] =
    newCartItem =>
      if (newCartItem.quantity <= 0)
        Either
          .left[CartUpdateError, CartItem](
            CartUpdateError("Invalid quantity: " + newCartItem.quantity.toString())
          )
          .pure[F]
      else
        toCartItem(newCartItem.itemId).flatMap {
          case Left(err) => Either.left[CartUpdateError, CartItem](err).pure[F]
          case Right(item) =>
            Either.right[CartUpdateError, CartItem](CartItem(item, newCartItem.quantity)).pure[F]
        }

  private def toCartItem: Int => F[Either[CartUpdateError, Item]] =
    itemId =>
      itemService.getItemById(itemId).flatMap {
        case None =>
          Either
            .left[CartUpdateError, Item](CartUpdateError("Invalid item id: " + itemId.toString()))
            .pure[F]
        case Some(item) => Either.right[CartUpdateError, Item](item).pure[F]
      }

  private def validateQuantity(item: NewCartItem): EitherT[F, CartUpdateError, Unit] =
    if (item.quantity <= 0)
      EitherT.left[Unit](CartUpdateError("Invalid quantity").pure[F])
    else
      EitherT.right[CartUpdateError](().pure[F])

  private def endpoints: HttpRoutes[F] = getUserCart <+> addToCart <+> deleteFromCart <+> updateCart

}

object CartEndpoint {

  def endpoints[F[_]: Async](
    cartService: CartService[F],
    itemService: ItemService[F],
    authValidation: AuthValidation[F],
  ): HttpRoutes[F] = new CartEndpoint[F](cartService, itemService, authValidation).endpoints

}
