package com.playground.shoppingcart.repository

import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import com.playground.shoppingcart.domain.company.Company

class CompanyRepository[F[_]: MonadCancelThrow](xa: Transactor[F]) {

    def getCompanies(): F[List[Company]] = 
        sql"SELECT id, name FROM company"
        .query[Company]
        .to[List]
        .transact(xa)


}