package com.playground.shoppingcart.domain.auth

import com.playground.shoppingcart.domain.user.Role
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

case class JWTClaim(userId: Long, username: String, role: String)

object JWTClaim {

  implicit val encoder: Encoder[JWTClaim] = deriveEncoder[JWTClaim]
  implicit val decoder: Decoder[JWTClaim] = deriveDecoder[JWTClaim]

}
