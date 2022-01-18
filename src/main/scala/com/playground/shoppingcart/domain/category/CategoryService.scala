package com.playground.shoppingcart.domain.category

import com.playground.shoppingcart.repository.CategoryRepository

class CategoryService[F[_]](categoryRepository: CategoryRepository[F]) {

  def getAll(): F[List[Category]] = categoryRepository.getAllCategories()

}
