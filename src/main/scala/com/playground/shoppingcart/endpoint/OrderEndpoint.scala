package com.playground.shoppingcart.endpoint

import cats._
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

class OrderEndpoint[F[_]: Async](
  cartService: CartService[F],
  authValidation: AuthValidation[F],
) extends Http4sDsl[F] {

  private val dtf = DateTimeFormatter.ofPattern("MM/uu")

  def checkout: HttpRoutes[F] = HttpRoutes.of[F] { case request @ POST -> Root / "checkout" =>
    authValidation.asAuthUser { authRequet =>
      val validate =
        for {
          checkoutR <- EitherT.liftF[F, CheckoutError, Checkout](request.as[Checkout])
          cart <- EitherT.liftF[F, CheckoutError, Cart](
            cartService.getUserCart(authRequet.authUser.id.get)
          )
          _ <-
            if (Cart.isEmpty(cart))
              EitherT.leftT[F, Unit](CheckoutError("Cart is empty"))
            else
              EitherT.rightT[F, CheckoutError](())
          _ <- validateCreditCard(checkoutR.creditCard, checkoutR.exp)
        } yield cart

      validate.value.flatMap {
        case Left(err) => BadRequest(err.msg)
        case Right(cart)  => Ok("ok")
      }
    }(request)
  }

  private def createPayment(payer: String, creditCard: String) = ???

  private def createOrder() = ???


  private def validateCreditCard(
    number: String,
    exp: String,
  ): EitherT[F, CheckoutError, Unit] =
    for {
      _ <- validateNumbers(number)
      _ <- validateCCExp(exp)
    } yield ()

  private def validateNumbers(number: String) =
    if (number.forall(_.isDigit))
      EitherT.rightT[F, CheckoutError](())
    else
      EitherT.leftT[F, Unit](CheckoutError("Invalid credit card"))

  private def validateCCExp(exp: String): EitherT[F, CheckoutError, Unit] =
    for {
      expiration <- parseExp(exp)
      current    <- EitherT.liftF[F, CheckoutError, YearMonth](Async[F].delay(YearMonth.now()))
      isValid <- EitherT.liftF[F, CheckoutError, Boolean](
        Async[F].delay(expiration.isAfter(current))
      )
      resp <-
        if (isValid)
          EitherT.rightT[F, CheckoutError](())
        else
          EitherT.leftT[F, Unit](CheckoutError("Credit Card expired"))
    } yield resp

  private def parseExp(exp: String): EitherT[F, CheckoutError, YearMonth] = EitherT(
    Async[F].delay(YearMonth.parse(exp, dtf)).attempt
  ).leftSemiflatMap(_ => CheckoutError("Expiration value invalid").pure[F])

  def endpoints: HttpRoutes[F] = checkout

}

object OrderEndpoint {

  def endpoints[F[_]: Async](
    cartService: CartService[F],
    authValidation: AuthValidation[F],
  ): HttpRoutes[F] = new OrderEndpoint[F](cartService, authValidation).endpoints

}
