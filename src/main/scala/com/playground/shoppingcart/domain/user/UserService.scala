package com.playground.shoppingcart.domain.user

import com.playground.shoppingcart.repository.UserRepository

case class UserService[F[_]](userRepository: UserRepository[F]) {
  
    def getUser(username: String): F[Option[User]] = 
        userRepository.getUserByUsername(username)

}
