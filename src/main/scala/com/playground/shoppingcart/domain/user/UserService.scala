package com.playground.shoppingcart.domain.user

import com.playground.shoppingcart.repository.UserRepository
import tsec.mac.jca.MacSigningKey
import tsec.mac.jca.HMACSHA256

case class UserService[F[_]](userRepository: UserRepository[F]) {

  def getUser(username: String): F[Option[User]] = userRepository.getUserByUsername(username)

  def createUser(user: User): F[Int] = userRepository.insertUser(user)

}
