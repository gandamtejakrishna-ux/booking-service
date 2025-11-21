package com.hotel.booking.models

import java.time.LocalDateTime
import play.api.libs.json._

case class Guest(id: Option[Int], firstName: String, lastName: String, email: String, phone: String, idProofUrl: Option[String])
object Guest { implicit val fmt = Json.format[Guest] }

case class Room(id: Int, floor: Int, roomNumber: Int, categoryId: Int, status: String)
object Room { implicit val fmt = Json.format[Room] }

case class RoomCategory(id: Int, name: String, price: BigDecimal)
object RoomCategory { implicit val fmt = Json.format[RoomCategory] }

case class Booking(
                    id: String,
                    guestId: Int,
                    roomId: Int,
                    checkInDate: Option[String],
                    checkOutDate: Option[String],
                    checkInTime: Option[String],
                    checkOutTime: Option[String],
                    status: String
                  )
object Booking { implicit val fmt = Json.format[Booking] }

case class OutboxEventRow(id: Long, aggregateId: Option[String], aggregateType: Option[String], eventType: String, payload: play.api.libs.json.JsValue, published: Boolean)
object OutboxEventRow { implicit val fmt = Json.format[OutboxEventRow] }

// request DTOs
case class CreateBookingRequest(guest: Guest, category: String, checkInDate: String, checkOutDate: String)
object CreateBookingRequest { implicit val fmt = Json.format[CreateBookingRequest] }

case class CheckInRequest(idProofUrl: Option[String], checkInTime: Option[String])
object CheckInRequest { implicit val fmt = Json.format[CheckInRequest] }

case class CheckOutRequest(checkOutTime: Option[String])
object CheckOutRequest { implicit val fmt = Json.format[CheckOutRequest] }
