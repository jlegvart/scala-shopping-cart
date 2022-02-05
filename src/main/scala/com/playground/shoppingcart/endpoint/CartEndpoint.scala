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
import com.playground.shoppingcart.domain.validation.CheckoutError

class CartEndpoint[F[_]](
  cartService: CartService[F],
  itemService: ItemService[F],
  authValidation: AuthValidation[F],
)(
  implicit F: Async[F]
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
      val addItem =
        for {
          newItem <- request.as[NewCartItem]
          _       <- validateQuantity(newItem)
          item    <- getItem(newItem.itemId)
          _       <- addItemToCart(authRequest.authUser.id, CartItem(item, newItem.quantity))
        } yield ()

      addItem
        .flatMap(_ => Created())
        .handleErrorWith {
          case CartUpdateError(msg) => BadRequest(msg)
          case _: MessageFailure    => BadRequest()
          case _                    => InternalServerError()
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
        } yield items

      cartItems
        .flatMap { items =>
          cartService.updateCartItems(authRequest.authUser.id, items) >> NoContent()
        }
        .handleErrorWith {
          case CartUpdateError(msg) => BadRequest(msg)
          case _: MessageFailure    => BadRequest()
          case _                    => InternalServerError()
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

  private def addItemToCart(
    userId: Int,
    cartItem: CartItem,
  ): F[Unit] = cartService.addToCart(userId, cartItem).flatMap {
    case Left(error) => F.raiseError(error)
    case Right(_)    => F.unit
  }

  private def asValidCartItem(newCartItem: NewCartItem): F[CartItem] =
    for {
      _        <- validateQuantity(newCartItem)
      item     <- getItem(newCartItem.itemId)
      cartItem <- CartItem(item, newCartItem.quantity).pure[F]
    } yield cartItem

  private def getItem(itemId: Int): F[Item] = itemService.getItemById(itemId).flatMap {
    case None       => F.raiseError(CartUpdateError("Invalid item id"))
    case Some(item) => F.pure(item)
  }

  private def validateQuantity(item: NewCartItem): F[Unit] =
    if (item.quantity <= 0)
      F.raiseError(CartUpdateError("Invalid quantity: " + item.quantity.toString()))
    else
      F.unit

  private def endpoints: HttpRoutes[F] = getUserCart <+> addToCart <+> deleteFromCart <+> updateCart

}

object CartEndpoint {

  def endpoints[F[_]: Async](
    cartService: CartService[F],
    itemService: ItemService[F],
    authValidation: AuthValidation[F],
  ): HttpRoutes[F] = new CartEndpoint[F](cartService, itemService, authValidation).endpoints

}
