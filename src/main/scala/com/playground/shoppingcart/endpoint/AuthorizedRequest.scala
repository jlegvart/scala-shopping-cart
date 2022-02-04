package com.playground.shoppingcart.endpoint

import org.http4s.Request
import com.playground.shoppingcart.domain.user.User
import com.playground.shoppingcart.domain.user.Role

final case class AuthorizedRequest[F[_]](request: Request[F], authUser: AuthorizedUser)
final case class AuthorizedUser(id: Int, username: String, role: Role)