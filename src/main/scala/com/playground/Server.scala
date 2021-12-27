package com.playground

import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.blaze.server._
import scala.concurrent.ExecutionContext.global
import io.circe.config.parser
import com.playground.config.ApplicationConfig

object Server extends IOApp {

  def createServer =
    for {
      config <- Resource.liftK[IO](ApplicationConfig.loadConfig[IO])
      _ <- Resource.liftK(IO.println(config))
      server <-
        BlazeServerBuilder[IO](global)
          .bindHttp(8080, "localhost")
          .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)

}
