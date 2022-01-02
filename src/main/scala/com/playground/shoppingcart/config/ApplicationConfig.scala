package com.playground.shoppingcart.config

import cats.effect.kernel.Sync
import io.circe._
import io.circe.generic.auto._
import io.circe.config.parser
import io.circe.syntax._
import io.circe.config.parser

final case class ApplicationConfig(db: DatabaseConfig, server: ServerConfig)
final case class ServerConfig(host: String, port: Int)

object ApplicationConfig {

  def loadConfig[F[_]: Sync](): F[ApplicationConfig] =
    (for {
      config <- parser.decodePath[ApplicationConfig]("scala-shopping-cart")
    } yield config) match {
      case Right(config) => Sync[F].pure(config)
      case Left(error)   => Sync[F].raiseError(error)
    }

}