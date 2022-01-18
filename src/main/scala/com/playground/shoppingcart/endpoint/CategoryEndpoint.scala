package com.playground.shoppingcart.endpoint

import com.playground.shoppingcart.domain.category.CategoryService
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.implicits._
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

class CategoryEndpoint[F[_]: Async](categoryService: CategoryService[F]) extends Http4sDsl[F] {

  private def listAllCategories: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    categoryService.getAll().flatMap(categories => Ok(categories.asJson))
  }

  def endpoints: HttpRoutes[F] = listAllCategories

}

object CategoryEndpoint {

  def endpoints[F[_]: Async](categoryService: CategoryService[F]): HttpRoutes[F] =
    new CategoryEndpoint[F](categoryService).endpoints

}
