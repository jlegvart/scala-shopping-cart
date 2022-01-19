package com.playground.shoppingcart.domain.item

import com.playground.shoppingcart.domain.company.Company
import com.playground.shoppingcart.domain.category.Category

final case class Item(id: Option[Int], company: Company, category: Category, name: String, price: BigDecimal)