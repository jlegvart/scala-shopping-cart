package com.playground.shoppingcart

import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.blaze.server._
import scala.concurrent.ExecutionContext.global
import io.circe.config.parser
import com.playground.shoppingcart.config.ApplicationConfig
import org.http4s.server.Router
import org.http4s.server.Server
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import com.playground.shoppingcart.config.DatabaseConfig
import com.playground.shoppingcart.endpoint.AuthEndpoint
import com.playground.shoppingcart.domain.user.UserService
import com.playground.shoppingcart.repository.UserRepository

object Server extends IOApp {

  def createServer[F[_]: Async]: Resource[F, Server] =
    for {
      config <- Resource.eval(ApplicationConfig.loadConfig[F]())
      transactor <- DatabaseConfig.transactor(config.db)
      _ <- Resource.eval(DatabaseConfig.initDB[F](config.db))
      userRepository = new UserRepository[F](transactor)
      userService = new UserService[F](userRepository)
      httpApp = Router("/" -> AuthEndpoint.endpoints(userService)).orNotFound
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
