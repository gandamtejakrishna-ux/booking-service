package com.hotel.booking.dao

import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag
import play.api.libs.json._
/**
 * DatabaseTables
 *
 * Contains Slick table mappings for all entities:
 * rooms, categories, guests, bookings, outbox, logs, schedules.
 * Also defines JSON → String column conversion support.
 */
object DatabaseTables {


  class RoomCategoriesTable(tag: Tag) extends Table[(Int, String, BigDecimal)](tag, "room_categories") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def price = column[BigDecimal]("price")

    def * = (id, name, price)
  }
  val roomCategories = TableQuery[RoomCategoriesTable]


  class RoomsTable(tag: Tag) extends Table[(Int, Int, Int, Int, String)](tag, "rooms") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def floor = column[Int]("floor")
    def roomNumber = column[Int]("room_number")
    def categoryId = column[Int]("category_id")
    def status = column[String]("status")
    def * = (id, floor, roomNumber, categoryId,status)
  }
  val rooms = TableQuery[RoomsTable]


  class GuestsTable(tag: Tag) extends Table[(Int, String, String, String, String, Option[String])](tag, "guests") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)

    def firstName = column[String]("first_name")
    def lastName = column[String]("last_name")
    def email = column[String]("email")
    def phone = column[String]("phone")
    def idProofUrl = column[Option[String]]("id_proof_url")

    def * = (id, firstName, lastName, email, phone, idProofUrl)
  }

  val guests = TableQuery[GuestsTable]

  class BookingsTable(tag: Tag)
    extends Table[(String, Int, Int, Option[java.sql.Date], Option[java.sql.Date], Option[java.sql.Timestamp], Option[java.sql.Timestamp], String)](
      tag,
      "bookings"
    ) {

    def id = column[String]("id", O.PrimaryKey)

    def guestId = column[Int]("guest_id")
    def roomId = column[Int]("room_id")

    def checkIn = column[Option[java.sql.Date]]("check_in")
    def checkOut = column[Option[java.sql.Date]]("check_out")

    def checkInTime = column[Option[java.sql.Timestamp]]("check_in_time")
    def checkOutTime = column[Option[java.sql.Timestamp]]("check_out_time")

    def status = column[String]("status")

    def * =
      (id, guestId, roomId, checkIn, checkOut, checkInTime, checkOutTime, status)
  }

  val bookings = TableQuery[BookingsTable]


  /** JSON support for Slick */
  implicit val jsonColumnType: BaseColumnType[JsValue] =
    MappedColumnType.base[JsValue, String](
      json => Json.stringify(json),
      str => Json.parse(str)
    )

  case class OutboxRow(
                        id: Long,
                        aggregateId: Option[String],
                        aggregateType: Option[String],
                        eventType: String,
                        payload: String,
                        published: Boolean
                      )

  class OutboxTable(tag: Tag)
    extends Table[OutboxRow](tag, "outbox_events") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def aggregateId = column[Option[String]]("aggregate_id")
    def aggregateType = column[Option[String]]("aggregate_type")
    def eventType = column[String]("event_type")
    def payload = column[String]("payload")
    def published = column[Boolean]("published")

    def * =
      (id, aggregateId, aggregateType, eventType, payload, published) <>
        (OutboxRow.tupled, OutboxRow.unapply)
  }

  val outbox = TableQuery[OutboxTable]


  class NotificationsLogTable(tag: Tag) extends Table[(Long, Option[String], String, Option[String], String, Option[String], Option[String], Option[String], java.sql.Timestamp, Option[java.sql.Timestamp])](tag, "notifications_log") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def bookingId = column[Option[String]]("booking_id")
    def eventType = column[String]("event_type")
    def recipient = column[Option[String]]("recipient")
    def channel = column[String]("channel")
    def template = column[Option[String]]("template")
    def payload = column[Option[String]]("payload")
    def status = column[Option[String]]("status")
    def createdAt = column[java.sql.Timestamp]("created_at")
    def sentAt = column[Option[java.sql.Timestamp]]("sent_at")

    def * = (id, bookingId, eventType, recipient, channel, template, payload, status, createdAt, sentAt)
  }
  val notificationsLog = TableQuery[NotificationsLogTable]


  class RestaurantSchedulesTable(tag: Tag) extends Table[(Long, String, String, Option[java.sql.Timestamp], Option[Int], Boolean, Option[String], java.sql.Timestamp, Option[java.sql.Timestamp])](tag, "restaurant_schedules") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def bookingId = column[String]("booking_id")
    def scheduleKey = column[String]("schedule_key")
    def nextRun = column[Option[java.sql.Timestamp]]("next_run")
    def repeatIntervalSec = column[Option[Int]]("repeat_interval_sec")
    def enabled = column[Boolean]("enabled")
    def payload = column[Option[String]]("payload")
    def createdAt = column[java.sql.Timestamp]("created_at")
    def updatedAt = column[Option[java.sql.Timestamp]]("updated_at")

    def * = (id, bookingId, scheduleKey, nextRun, repeatIntervalSec, enabled, payload, createdAt, updatedAt)
  }
  val restaurantSchedules = TableQuery[RestaurantSchedulesTable]

}
