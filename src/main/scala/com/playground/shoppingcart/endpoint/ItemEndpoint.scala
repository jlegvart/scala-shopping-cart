package com.playground.shoppingcart.endpoint

import cats.effect.kernel.Async
import com.playground.shoppingcart.domain.item.ItemService
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.implicits._
import cats._
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

class ItemEndpoint[F[_]: Async](itemService: ItemService[F]) extends Http4sDsl[F] {

  private def listAllItemsEndpoint(): HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    itemService.getAllItems().flatMap(items => Ok(items.asJson))
  }

  def endpoints: HttpRoutes[F] = listAllItemsEndpoint()
}

object ItemEndpoint {

  def endpoints[F[_]: Async](itemService: ItemService[F]) =
    new ItemEndpoint[F](itemService).endpoints

}
