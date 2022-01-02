package com.playground.shoppingcart.config

import doobie.util.ExecutionContexts
import doobie.hikari.HikariTransactor
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import cats.effect.kernel.Sync
import org.flywaydb.core.Flyway

final case class Connections(poolSize: Int)

final case class DatabaseConfig(
  url: String,
  user: String,
  password: String,
  driver: String,
  connections: Connections,
)

object DatabaseConfig {

  def transactor[F[_]: Async](dbConfig: DatabaseConfig): Resource[F, HikariTransactor[F]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[F](32)
      xa <- HikariTransactor.newHikariTransactor[F](
        dbConfig.driver,
        dbConfig.url,
        dbConfig.user,
        dbConfig.password,
        ce,
      )
    } yield xa

  def initDB[F[_]: Sync](dbConfig: DatabaseConfig): F[Unit] = Sync[F].delay {
    Flyway
      .configure()
      .dataSource(dbConfig.url, dbConfig.user, dbConfig.password)
      .load()
      .migrate()
  }

}
