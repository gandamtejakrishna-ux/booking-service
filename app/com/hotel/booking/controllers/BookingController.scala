package com.hotel.booking.controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import com.hotel.booking.services.BookingService
import com.hotel.booking.models._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * BookingController
 *
 * Exposes REST API endpoints for booking operations.
 * Delegates business logic to BookingService.
 */
@Singleton
class BookingController @Inject()(cc: ControllerComponents, bookingService: BookingService)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  /**
   * POST /internal/bookings
   * Creates a new booking after validating request payload.
   */
  def createBooking = Action.async(parse.json) { req =>
    req.body.validate[CreateBookingRequest].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors)))),
      reqDto => {
        bookingService.createBooking(reqDto).map { bookingId =>
          Created(Json.obj("bookingId" -> bookingId, "status" -> "BOOKED"))
        }.recover {
          case ex: Exception => InternalServerError(Json.obj("error" -> ex.getMessage))
        }
      }
    )
  }

  /**
   * POST /internal/bookings/:id/checkin
   * Marks booking as CHECKED_IN and updates check-in timestamp.
   */
  def checkIn(id: String) = Action.async(parse.json) { req =>
    req.body.validate[CheckInRequest].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors)))),
      dto => bookingService.checkIn(id, dto).map(_ => Ok(Json.obj("bookingId" -> id, "status" -> "CHECKED_IN"))).recover {
        case ex: Exception => InternalServerError(Json.obj("error" -> ex.getMessage))
      }
    )
  }

  /**
   * POST /internal/bookings/:id/checkout
   * Marks booking as CHECKED_OUT and updates checkout timestamp.
   */
  def checkOut(id: String) = Action.async(parse.json) { req =>
    req.body.validate[CheckOutRequest].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors)))),
      dto => bookingService.checkOut(id, dto).map(_ => Ok(Json.obj("bookingId" -> id, "status" -> "CHECKED_OUT"))).recover {
        case ex: Exception => InternalServerError(Json.obj("error" -> ex.getMessage))
      }
    )
  }

  /**
   * GET /internal/rooms
   * Returns list of all rooms.
   */
  def listRooms = Action.async {
    bookingService.listRooms().map(rooms => Ok(Json.toJson(rooms)))
  }

  /**
   * GET /internal/rooms/availability
   * Returns rooms available for given date and category.
   */
  def getAvailability(date: String, category: String) = Action.async {
    bookingService.getAvailability(date, category).map(rooms => Ok(Json.toJson(rooms)))
  }

  /**
   * GET /internal/bookings/:id
   * Returns booking details by ID.
   */
  def getBooking(id: String) = Action.async {
    bookingService.getBooking(id).map {
      case Some(b) => Ok(Json.toJson(b))
      case None => NotFound(Json.obj("error" -> "Not found"))
    }
  }
}
