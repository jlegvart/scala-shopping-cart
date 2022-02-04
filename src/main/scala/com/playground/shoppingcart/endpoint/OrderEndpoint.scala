package com.playground.shoppingcart.endpoint

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
import com.playground.shoppingcart.domain.validation.CheckoutError
import java.time.format.DateTimeFormatter
import java.time.YearMonth
import cats.data.EitherT
import com.playground.shoppingcart.domain.cart.Cart
import com.playground.shoppingcart.domain.payment.Payment
import com.playground.shoppingcart.domain.order.OrderService
import com.playground.shoppingcart.domain.payment.PaymentService
import com.playground.shoppingcart.domain.order.OrderItem
import com.playground.shoppingcart.domain.order.Order

class OrderEndpoint[F[_]](
  cartService: CartService[F],
  orderService: OrderService[F],
  paymentService: PaymentService[F],
  authValidation: AuthValidation[F],
)(
  implicit F: Async[F]
) extends Http4sDsl[F] {

  private val dtf = DateTimeFormatter.ofPattern("MM/uu")

  def checkout: HttpRoutes[F] = HttpRoutes.of[F] { case request @ POST -> Root / "checkout" =>
    authValidation.asAuthUser { authRequet =>
      val cartWithCheckout =
        for {
          checkout <- request.as[Checkout]
          cart     <- cartService.getUserCart(authRequet.authUser.id)
          _        <- cartIsNotEmpty(cart)
          _        <- validateCreditCard(checkout.creditCard, checkout.exp)
        } yield (cart, checkout)

      cartWithCheckout
        .flatMap { cartWithCheckout =>
          val (cart, checkout) = cartWithCheckout

          for {
            order <- executeOrder(cart, checkout, authRequet.authUser.id)
            _     <- cartService.clearCart(order.userId)
            id = order.id.getOrElse(0)
            ok <- Ok(id.asJson)
          } yield ok
        }
        .handleErrorWith {
          case CheckoutError(msg) => BadRequest(msg)
          case _                  => InternalServerError("Error during checkout operation")
        }
    }(request)
  }

  private def cartIsNotEmpty(cart: Cart): F[Unit] =
    if (Cart.isEmpty(cart))
      F.raiseError(CheckoutError("Cart is empty"))
    else
      F.unit

  private def executeOrder(
    cart: Cart,
    checkout: Checkout,
    userId: Int,
  ): F[Order] = {
    val orderItems = cart
      .items
      .map(item => OrderItem(None, item.item.id.get, item.quantity, item.item.price))

    paymentService
      .executePayment(checkout.payer, checkout.creditCard)
      .flatMap(payment => createOrder(userId, orderItems, payment, cart.total))
  }

  private def createOrder(
    userId: Int,
    orderItems: List[OrderItem],
    payment: Payment,
    total: BigDecimal,
  ): F[Order] = orderService.create(Order(None, None, userId, orderItems, total), payment).flatMap {
    case Left(error)  => F.raiseError(error)
    case Right(order) => F.pure(order)
  }

  private def validateCreditCard(
    number: String,
    exp: String,
  ): F[Unit] = validateCCNumbers(number) *> validateCCExp(exp)

  private def validateCCNumbers(number: String): F[Unit] =
    if (number.forall(_.isDigit))
      F.unit
    else
      F.raiseError(CheckoutError("Invalid credit card number"))

  private def validateCCExp(exp: String): F[Unit] =
    for {
      expiration <- parseExp(exp)
      current    <- Async[F].delay(YearMonth.now())
      isValid    <- Async[F].delay(expiration.isAfter(current))
      _ <-
        if (isValid)
          F.unit
        else
          F.raiseError(CheckoutError("Credit card expired"))
    } yield ()

  private def parseExp(exp: String): F[YearMonth] = F
    .delay(YearMonth.parse(exp, dtf))
    .handleErrorWith(_ => F.raiseError(CheckoutError("Invalid expiration value")))

  def endpoints: HttpRoutes[F] = checkout

}

object OrderEndpoint {

  def endpoints[F[_]: Async](
    cartService: CartService[F],
    orderService: OrderService[F],
    paymentService: PaymentService[F],
    authValidation: AuthValidation[F],
  ): HttpRoutes[F] =
    new OrderEndpoint[F](cartService, orderService, paymentService, authValidation).endpoints

}
