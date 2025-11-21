package com.hotel.booking.kafka

import javax.inject._
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization.StringSerializer
import play.api.Configuration
import play.api.libs.json._
import scala.concurrent.{Future, Promise}
import scala.concurrent.ExecutionContext
import org.slf4j.LoggerFactory

@Singleton
class KafkaPublisher @Inject()(config: Configuration)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val bootstrapServers = config.get[String]("kafka.bootstrap.servers")
  private val topic = config.get[String]("kafka.outbox.topic")

  private val props = {
    val p = new java.util.Properties()
    p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    p
  }

  private val producer = new KafkaProducer[String, String](props)

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

//  def shutdown(): Unit = {
//    logger.info("Shutting down Kafka producer.")
//    producer.flush()
//    producer.close()
//  }
}
