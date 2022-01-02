package com.playground.shoppingcart.config

final case class Connections(poolSize: Int)
final case class DatabaseConfig(url: String, user: String, password: String, driver: String, connections: Connections)