package com.hotel.booking.outbox

import javax.inject._
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import play.api.inject.ApplicationLifecycle
import com.hotel.booking.dao.DbConfig
import com.hotel.booking.kafka.KafkaPublisher
import slick.jdbc.MySQLProfile.api._
import org.slf4j.LoggerFactory
import play.api.libs.json._

/**
 * Periodically polls the outbox_events table and publishes pending events to Kafka.
 * Once published successfully, marks the event as published (published = TRUE).
 *
 * The scheduler runs every 3 seconds and processes up to 20 events per batch.
 *
 * @param dbConfig  Database configuration for running Slick queries
 * @param publisher Kafka publisher used to send JSON events
 * @param lifecycle Ensures scheduler stops when application shuts down
 */

@Singleton
class OutboxScheduler @Inject()(
                                 dbConfig: DbConfig,
                                 publisher: KafkaPublisher,
                                 lifecycle: ApplicationLifecycle
                               )(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val db = dbConfig.database

  private val scheduler = akka.actor.ActorSystem("outbox-system").scheduler

  // Poll every 3 seconds
  private val cancellable =
    scheduler.scheduleAtFixedRate(5.seconds, 3.seconds)(() => poll())

  lifecycle.addStopHook { () =>
    cancellable.cancel()
    Future.successful(())
  }

  /**
   * Fetches all unpublished outbox events (published = FALSE),
   * attaches eventType to the payload JSON and sends them for publishing.
   */

  private def poll(): Unit = {

    // Fetch event_type also (IMPORTANT!)
    val fetchQuery =
      sql"""
          SELECT id, event_type, payload
          FROM outbox_events
          WHERE published = FALSE
          ORDER BY id
          LIMIT 20
         """
        .as[(Long, String, String)]

    db.run(fetchQuery).map { rows =>
      rows.foreach { case (id, eventType, payloadStr) =>

        val baseJson = Json.parse(payloadStr).as[JsObject]

        // Inject eventType into JSON
        val enrichedJson = baseJson ++ Json.obj("eventType" -> eventType)

        publishEvent(id, enrichedJson)
      }
    }
  }

  /**
   * Publishes an event to Kafka.
   * If publish succeeds → updates published = TRUE in database.
   * If publish fails → logs error and retry will happen in next poll cycle.
   *
   * @param id      Outbox row ID
   * @param payload JSON payload to publish
   */
  private def publishEvent(id: Long, payload: JsValue): Unit = {
    publisher.publish(payload).map { _ =>
      val updateQuery =
        sqlu"UPDATE outbox_events SET published = TRUE WHERE id = $id"

      db.run(updateQuery).map { _ =>
        logger.info(s"[Outbox] Event $id marked as published")
      }
    }.recover { case ex =>
      logger.error(s"[Outbox] Failed to publish $id — will retry: ${ex.getMessage}")
    }
  }
}
