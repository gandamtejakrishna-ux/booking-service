package com.hotel.booking.controllers

import javax.inject._
import play.api.mvc._

@Singleton
class HealthController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  def health = Action {
    Ok("booking-service OK")
  }
}
