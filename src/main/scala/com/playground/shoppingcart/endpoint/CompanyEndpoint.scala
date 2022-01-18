package com.playground.shoppingcart.endpoint

import com.playground.shoppingcart.domain.company.CompanyService
import com.playground.shoppingcart.domain.company.Company
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.implicits._
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import io.circe.syntax._
import io.circe.generic.auto._

class CompanyEndpoint[F[_]: Async](companyService: CompanyService[F]) extends Http4sDsl[F] {

  private def listCompaniesEndpoint: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
      companyService.getAll().flatMap(companies => Ok(companies.asJson))
  }

  def endpoints: HttpRoutes[F] = listCompaniesEndpoint
}

object CompanyEndpoint {

  def endpoints[F[_]: Async](companyService: CompanyService[F]): HttpRoutes[F] =
    new CompanyEndpoint[F](companyService).endpoints

}
