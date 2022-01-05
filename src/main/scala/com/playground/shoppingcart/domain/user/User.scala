package com.playground.shoppingcart.domain.user

import doobie.util.meta.Meta

trait Role {
  def name: String
}

object Customer extends Role {
  val name = "CUSTOMER"
}

object Admin extends Role {
  val name = "ADMIN"
}

object Role {

  def toRole(n: String): Role =
    n match {
      case Customer.name => Customer
      case Admin.name    => Admin
    }

  def fromRole(role: Role): String =
    role match {
      case Customer => Customer.name
      case Admin    => Admin.name
    }

  implicit val roleMeta: Meta[Role] = Meta[String].imap(toRole)(fromRole)
}

case class User(id: Option[Int], username: String, password: String, role: Role)
