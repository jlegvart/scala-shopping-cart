package com.playground.shoppingcart.domain.company

import com.playground.shoppingcart.repository.CompanyRepository

class CompanyService[F[_]](companyRepository: CompanyRepository[F]) {

    def getAll(): F[List[Company]] = 
        companyRepository.getCompanies()

}