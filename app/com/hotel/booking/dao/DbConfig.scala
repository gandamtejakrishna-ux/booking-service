package com.hotel.booking.dao

import javax.inject._
import slick.jdbc.JdbcProfile
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import scala.concurrent.ExecutionContext

/**
 * Provides access to the Slick database configuration for the Booking Service.
 *
 * <p>This class loads the JDBC profile and database instance using Play's
 * DatabaseConfigProvider. Other services and DAOs inject this class to
 * execute Slick queries.</p>
 *
 * @constructor Injects the Play DatabaseConfigProvider.
 * @param dbConfigProvider Provider for database configuration.
 */
@Singleton
class DbConfig @Inject()(
                          protected val dbConfigProvider: DatabaseConfigProvider
                        )(implicit val ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

 // val jdbcProfile: JdbcProfile = profile

  // Make db visible to other classes
  /** Exposes the configured Slick database instance for executing DB actions. */
  val database = db
}
