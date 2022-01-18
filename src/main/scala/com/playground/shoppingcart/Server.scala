package com.playground.shoppingcart

import cats.effect._
import com.playground.shoppingcart.config.ApplicationConfig
import com.playground.shoppingcart.config.DatabaseConfig
import com.playground.shoppingcart.domain.user.UserService
import com.playground.shoppingcart.domain.company.CompanyService
import com.playground.shoppingcart.endpoint.AuthEndpoint
import com.playground.shoppingcart.endpoint.CompanyEndpoint
import com.playground.shoppingcart.repository.UserRepository
import com.playground.shoppingcart.repository.CompanyRepository
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import io.circe.config.parser
import org.http4s.HttpRoutes
import org.http4s.blaze.server._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.Server
import tsec.jws.mac._
import tsec.jwt._
import tsec.mac.jca._

import scala.concurrent.ExecutionContext.global

object Server extends IOApp {

  def createServer[F[_]: Async]: Resource[F, Server] =
    for {
      config     <- Resource.eval(ApplicationConfig.loadConfig[F]())
      transactor <- DatabaseConfig.transactor(config.db)
      _          <- Resource.eval(DatabaseConfig.initDB[F](config.db))
      key        <- Resource.liftK(HMACSHA256.generateKey[F])
      userRepository    = new UserRepository[F](transactor)
      companyRepository = new CompanyRepository[F](transactor)
      userService       = new UserService[F](userRepository)
      companyService    = new CompanyService[F](companyRepository)
      httpApp =
        Router(
          "/"          -> AuthEndpoint.endpoints(userService, key),
          "/companies" -> CompanyEndpoint.endpoints[F](companyService),
        ).orNotFound
      server <-
        BlazeServerBuilder[F](global)
          .bindHttp(config.server.port, config.server.host)
          .withHttpApp(httpApp)
          .resource
    } yield server

  def run(
    args: List[String]
  ): IO[ExitCode] = createServer[IO].use(_ => IO.never).as(ExitCode.Success)

}
