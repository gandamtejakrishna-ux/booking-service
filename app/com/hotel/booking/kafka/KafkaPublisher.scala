package com.hotel.booking.kafka

import javax.inject._
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization.StringSerializer
import play.api.Configuration
import play.api.libs.json._
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext
import org.slf4j.LoggerFactory

/**
 * KafkaPublisher is a simple wrapper around KafkaProducer that publishes
 * JSON events to the Outbox Kafka topic.
 *
 * - Reads Kafka configuration from application.conf
 * - Sends messages asynchronously
 * - Returns a Future to indicate publish success/failure
 */
@Singleton
class KafkaPublisher @Inject()(config: Configuration)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  /** Kafka bootstrap server address */
  private val bootstrapServers = config.get[String]("kafka.bootstrap.servers")
  /** Kafka topic where outbox events are published */
  private val topic = config.get[String]("kafka.outbox.topic")

  /** Kafka producer configuration (string key, string value) */
  private val props = {
    val p = new java.util.Properties()
    p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    p
  }

  /** Shared Kafka producer instance */
  private val producer = new KafkaProducer[String, String](props)

  /**
   * Publishes a JSON event to Kafka asynchronously.
   *
   * @param payload JSON payload to send
   * @return Future[Unit] succeeds only if Kafka ACK is received
   */
  def publish(payload: JsValue): Future[Unit] = {
    val promise = Promise[Unit]()
    val record = new ProducerRecord[String, String](topic, payload.toString())

    producer.send(record, (metadata, exception) => {
      if (exception != null) {
        logger.error(s"Kafka publish failed: ${exception.getMessage}")
        promise.failure(exception)
      } else {
        logger.info(s"Published to Kafka topic=$topic partition=${metadata.partition()} offset=${metadata.offset()}")
        promise.success(())
      }
    })

    promise.future
  }


//  lifecycle.addStopHook { () =>
//    Future {
//      shutdown()
//    }
//  }
//  def shutdown(): Unit = {
//    logger.info("Shutting down Kafka producer.")
//    producer.flush()
//    producer.close()
//  }
}
