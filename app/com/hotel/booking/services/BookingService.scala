package com.hotel.booking.services

import javax.inject._
import com.hotel.booking.dao.DatabaseTables._
import com.hotel.booking.dao.DbConfig
import com.hotel.booking.kafka.KafkaPublisher
import com.hotel.booking.models._
import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import play.api.libs.json._
import java.sql.{Date, Timestamp}
import org.slf4j.LoggerFactory

@Singleton
class BookingService @Inject()(
                                dbConfig: DbConfig
                              )(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val db = dbConfig.database

  private def findAvailableRoomId(
                                   categoryName: String,
                                   checkIn: Date,
                                   checkOut: Date
                                 ): DBIO[Option[Int]] = {

    val roomsInCategory = for {
      rc <- roomCategories if (rc.name === categoryName)
      r <- rooms if r.categoryId === rc.id && r.status === "AVAILABLE"
    } yield r.id

    val occupied = bookings.filter(b =>
      b.checkIn.isDefined && b.checkOut.isDefined &&
        (b.checkIn < checkOut.bind) &&
        (b.checkOut > checkIn.bind) &&
        (b.status =!= "CHECKED_OUT") &&
        (b.status =!= "CANCELLED")).map(_.roomId)
    roomsInCategory.filterNot(rid => occupied.filter(_ === rid).exists).take(1).result.headOption
  }

  //  CREATE BOOKING
  def createBooking(req: CreateBookingRequest): Future[String] = {
    val bookingId = UUID.randomUUID().toString
    val guest = req.guest

    val payloadJson = Json.obj(
      "eventType" -> "BOOKING_CREATED",
      "bookingId" -> bookingId,
      "guest" -> Json.toJson(guest),
      "category" -> req.category,
      "checkInDate" -> req.checkInDate,
      "checkOutDate" -> req.checkOutDate
    )

    val inDate = Date.valueOf(req.checkInDate)
    val outDate = Date.valueOf(req.checkOutDate)

    val action = for {
      // Inserting guest
      guestId <- (guests returning guests.map(_.id)) += (
        0,
        guest.firstName,
        guest.lastName,
        guest.email,
        guest.phone,
        guest.idProofUrl
      )

      // Room allocation
      optRoomId <- findAvailableRoomId(req.category, inDate, outDate)
      roomId <- optRoomId match {
        case Some(id) => DBIO.successful(id)
        case None     => DBIO.failed(new Exception("No room available for selected category & date range"))

      }

      // Inserting booking
      _ <- bookings += (
        bookingId,
        guestId,
        roomId,
        Some(inDate),
        Some(outDate),
        None,
        None,
        "CREATED"
      )
      _ <- rooms.filter(_.id === roomId).map(_.status).update("OCCUPIED")

      // Inserting outbox
      outboxId <- (outbox returning outbox.map(_.id)) +=
        OutboxRow(
          0L,
          Some(bookingId),
          Some("Booking"),
          "BOOKING_CREATED",
          Json.stringify(payloadJson),
          false
        )

    } yield (bookingId, outboxId)

    db.run(action.transactionally).map { case (bid, oid) =>
      logger.info(s"Created booking = $bid (outbox event = $oid)")
      bid
    }
  }

  //  CHECK-IN
  def checkIn(bookingId: String, req: CheckInRequest): Future[Unit] = {

    val payloadJson = Json.obj(
      "eventType" -> "CHECKED_IN",
      "bookingId" -> bookingId,
      "idProofUrl" -> req.idProofUrl,
      "checkInTime" -> req.checkInTime
    )

    val action = for {
      // Parse ISO → Timestamp safely
      parsedTs <- DBIO.successful(
        req.checkInTime.map { tsStr =>
          val normalized = tsStr.replace("T", " ")
          Timestamp.valueOf(normalized)
        }
      )

      // Update the booking
      _ <- bookings
        .filter(_.id === bookingId)
        .map(b => (b.status, b.checkInTime))
        .update(("CHECKED_IN", parsedTs))

      // Fetching guestId
      bRowOpt <- bookings.filter(_.id === bookingId).result.headOption
      _ <- bRowOpt match {
        case Some((_, guestId, _, _, _, _, _, _)) =>
          req.idProofUrl match {
            case Some(url) =>
              guests.filter(_.id === guestId).map(_.idProofUrl).update(Some(url))
            case None =>
              DBIO.successful(0)
          }
        case None =>
          DBIO.failed(new Exception("Booking not found"))
      }

      // Inserting outbox
      _ <- (outbox returning outbox.map(_.id)) +=
        OutboxRow(
          0L,
          Some(bookingId),
          Some("Booking"),
          "CHECKED_IN",
          Json.stringify(payloadJson),
          false
        )

    } yield ()

    db.run(action.transactionally)
  }

  //  CHECK-OUT
  def checkOut(bookingId: String, req: CheckOutRequest): Future[Unit] = {

    val payloadJson = Json.obj(
      "eventType" -> "CHECKED_OUT",
      "bookingId" -> bookingId,
      "checkOutTime" -> req.checkOutTime
    )

    val action = for {
      parsedTs <- DBIO.successful(
        req.checkOutTime.map(ts => Timestamp.valueOf(ts))
      )

      // Update status + time
      _ <- bookings
        .filter(_.id === bookingId)
        .map(b => (b.status, b.checkOutTime))
        .update(("CHECKED_OUT", parsedTs))

      bookingRowOpt <- bookings.filter(_.id === bookingId).result.headOption
      roomId <- bookingRowOpt match {
        case Some((_, _, roomId, _, _, _, _, _)) =>
          DBIO.successful(roomId)
        case None =>
          DBIO.failed(new Exception("Booking not found"))
      }

      _ <- rooms.filter(_.id === roomId).map(_.status).update("CLEANING")

      // Insert outbox
      _ <- (outbox returning outbox.map(_.id)) +=
        OutboxRow(
          0L,
          Some(bookingId),
          Some("Booking"),
          "CHECKED_OUT",
          Json.stringify(payloadJson),
          false
        )

    } yield ()

    db.run(action.transactionally)
  }

  //  GET BOOKING
  def getBooking(bookingId: String): Future[Option[Booking]] = {
    db.run(bookings.filter(_.id === bookingId).result.headOption).map {
      case Some((id, guestId, roomId, cid, cod, cit, cot, status)) =>
        Some(
          Booking(
            id,
            guestId,
            roomId,
            cid.map(_.toString),
            cod.map(_.toString),
            cit.map(_.toString),
            cot.map(_.toString),
            status
          )
        )
      case None => None
    }
  }

  //  LIST ROOMS
  def listRooms(): Future[Seq[Room]] = {
    db.run(rooms.result).map(_.map {
      case (id, floor, number, catId, status) => Room(id, floor, number, catId, status)
    })
  }

  //  AVAILABILITY (simple)
  def getAvailability(date: String, category: String): Future[Seq[Room]] = {
    val q = for {
      rc <- roomCategories if rc.name === category
      r  <- rooms if r.categoryId === rc.id
    } yield r

    db.run(q.result).map(_.map {
      case (id, floor, n, cat, status) =>
        Room(id, floor, n, cat, status)

    })
  }
}
