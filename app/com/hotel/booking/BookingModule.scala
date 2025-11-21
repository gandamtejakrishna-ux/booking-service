package com.hotel.booking

import play.api.inject.{SimpleModule, _}
import com.hotel.booking.outbox.OutboxScheduler

class BookingModule extends SimpleModule(
  bind[OutboxScheduler].toSelf.eagerly()
)
