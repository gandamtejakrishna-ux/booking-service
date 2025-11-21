package com.hotel.booking.dao

import javax.inject._
import slick.jdbc.JdbcProfile
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import scala.concurrent.ExecutionContext

@Singleton
class DbConfig @Inject()(
                          protected val dbConfigProvider: DatabaseConfigProvider
                        )(implicit val ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

 // val jdbcProfile: JdbcProfile = profile

  // Make db visible to other classes
  val database = db
}
