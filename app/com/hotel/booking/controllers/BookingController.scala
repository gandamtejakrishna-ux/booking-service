package com.hotel.booking.controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import com.hotel.booking.services.BookingService
import com.hotel.booking.models._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class BookingController @Inject()(cc: ControllerComponents, bookingService: BookingService)(implicit ec: ExecutionContext) extends AbstractController(cc) {

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

  def checkIn(id: String) = Action.async(parse.json) { req =>
    req.body.validate[CheckInRequest].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors)))),
      dto => bookingService.checkIn(id, dto).map(_ => Ok(Json.obj("bookingId" -> id, "status" -> "CHECKED_IN"))).recover {
        case ex: Exception => InternalServerError(Json.obj("error" -> ex.getMessage))
      }
    )
  }

  def checkOut(id: String) = Action.async(parse.json) { req =>
    req.body.validate[CheckOutRequest].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors)))),
      dto => bookingService.checkOut(id, dto).map(_ => Ok(Json.obj("bookingId" -> id, "status" -> "CHECKED_OUT"))).recover {
        case ex: Exception => InternalServerError(Json.obj("error" -> ex.getMessage))
      }
    )
  }

  def listRooms = Action.async {
    bookingService.listRooms().map(rooms => Ok(Json.toJson(rooms)))
  }

  def getAvailability(date: String, category: String) = Action.async {
    bookingService.getAvailability(date, category).map(rooms => Ok(Json.toJson(rooms)))
  }

  def getBooking(id: String) = Action.async {
    bookingService.getBooking(id).map {
      case Some(b) => Ok(Json.toJson(b))
      case None => NotFound(Json.obj("error" -> "Not found"))
    }
  }
}
