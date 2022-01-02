package com.playground.shoppingcart

import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.blaze.server._
import scala.concurrent.ExecutionContext.global
import io.circe.config.parser
import com.playground.shoppingcart.config.ApplicationConfig
import org.http4s.server
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import com.playground.shoppingcart.config.DatabaseConfig

object Server extends IOApp {

  def createServer[F[_]: Async]: Resource[F, server.Server] =
    for {
      config <- Resource.eval(ApplicationConfig.loadConfig[F]())
      transactor <- DatabaseConfig.transactor(config.db)
      _ <- Resource.eval(DatabaseConfig.initDB[F](config.db))
      server <-
        BlazeServerBuilder[F](global)
          .bindHttp(config.server.port, config.server.host)
          .resource
    } yield server

  def run(
    args: List[String]
  ): IO[ExitCode] = createServer[IO].use(_ => IO.never).as(ExitCode.Success)

}
