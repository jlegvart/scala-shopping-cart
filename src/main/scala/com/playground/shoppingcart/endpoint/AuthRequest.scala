package com.playground.shoppingcart.endpoint

import org.http4s.Request
import com.playground.shoppingcart.domain.user.User

final case class AuthRequest[F[_]](request: Request[F], authUser: User)