package com.playground.shoppingcart

import cats.effect._
import com.playground.shoppingcart.config.ApplicationConfig
import com.playground.shoppingcart.config.DatabaseConfig
import com.playground.shoppingcart.domain.cart.Cart
import com.playground.shoppingcart.domain.cart.CartService
import com.playground.shoppingcart.domain.category.CategoryService
import com.playground.shoppingcart.domain.company.CompanyService
import com.playground.shoppingcart.domain.item.ItemService
import com.playground.shoppingcart.domain.user.UserService
import com.playground.shoppingcart.endpoint.AuthEndpoint
import com.playground.shoppingcart.endpoint.CartEndpoint
import com.playground.shoppingcart.endpoint.CategoryEndpoint
import com.playground.shoppingcart.endpoint.CompanyEndpoint
import com.playground.shoppingcart.endpoint.ItemEndpoint
import com.playground.shoppingcart.repository.CartRepository
import com.playground.shoppingcart.repository.CategoryRepository
import com.playground.shoppingcart.repository.CompanyRepository
import com.playground.shoppingcart.repository.ItemRepository
import com.playground.shoppingcart.repository.UserRepository
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
      store      <- Resource.liftK(Ref[F].of(Map.empty[Int, Cart]))
      userRepository     = new UserRepository[F](transactor)
      companyRepository  = new CompanyRepository[F](transactor)
      categoryRepository = new CategoryRepository[F](transactor)
      itemRepository     = new ItemRepository[F](transactor)
      cartRepository     = new CartRepository[F](store)
      userService        = new UserService[F](userRepository)
      companyService     = new CompanyService[F](companyRepository)
      categoryService    = new CategoryService[F](categoryRepository)
      itemService        = new ItemService[F](itemRepository)
      cartService        = new CartService[F](cartRepository)
      httpApp =
        Router(
          "/"           -> AuthEndpoint.endpoints(userService, key),
          "/companies"  -> CompanyEndpoint.endpoints[F](companyService),
          "/categories" -> CategoryEndpoint.endpoints[F](categoryService),
          "/items"      -> ItemEndpoint.endpoints[F](itemService),
          "/cart"       -> CartEndpoint.endpoints[F](cartService),
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
