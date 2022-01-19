package com.playground.shoppingcart.domain.item

import com.playground.shoppingcart.repository.ItemRepository

class ItemService[F[_]](itemRepository: ItemRepository[F]) {
  
    def getAllItems(): F[List[Item]] = 
        itemRepository.getAll()

}
