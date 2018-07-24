package com.esri.realtime.dse

import java.util.UUID

import com.datastax.driver.core.ConsistencyLevel
import com.datastax.spark.connector._
import com.datastax.spark.connector.cql.CassandraConnector
import com.fasterxml.jackson.dataformat.csv.{CsvMapper, CsvParser, CsvSchema}
import org.apache.commons.logging.LogFactory
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.{Milliseconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.Random

//
// Spark-DSE-Connector:  https://github.com/datastax/spark-cassandra-connector
//
// NOTE TO SELF: DON'T use the App trait!
// http://www.puroguramingu.com/2016/02/26/spark-dos-donts.html
//
object KafkaToDSE{

  private val log = LogFactory.getLog(this.getClass)

  /**
    * Main Method
    */
  def main(args: Array[String]): Unit = {

    if (args.length < 10) {
      System.err.println("Usage: KafkaToDSE <sparkMaster> <emitIntervalInMillis>" +
          " <kafkaConsumerGroup> <kafkaTopics> <kafkaThreads> <cassandraHost> <replicationFactor> <storeGeo> <debug>")
      System.exit(1)
    }

    val Array(sparkMaster, emitInterval, kBrokers, kConsumerGroup, kTopics, kThreads, kCassandraHost, kReplicationFactor, storeGeo, kDebug) = args
    val useSolr = storeGeo.toBoolean
    println("Using Solr ? " + useSolr)

    // configuration
    val sConf = new SparkConf(true)
        .set("spark.cassandra.connection.host", kCassandraHost)
        .set("spark.cassandra.output.concurrent.writes", "20")              // default is 5
        .set("spark.cassandra.output.batch.size.bytes", "1024000")          // default is 1024
        .set("spark.cassandra.output.batch.grouping.buffer.size", "10000")  // default is 1000

        //.set("spark.cassandra.connection.keep_alive_ms", "10000") //default is 250
        //.set("spark.cassandra.output.batch.size.rows", "1000") // default is "auto"
        //.set("spark.cassandra.output.batch.size.bytes", (1000000).toString) //default is 16 KB
        //.set("spark.cassandra.output.concurrent.writes", "1000") // default is "8"
        //.set("spark.cassandra.output.batch.grouping.buffer.size", "2000") // default is 1000
        //.set("spark.cassandra.output.throughput_mb_per_sec", Int.MaxValue.toString)   //default: Int.MaxValue.
        //.set("spark.cassandra.output.metrics", "true")            // default: true
        .set("spark.cassandra.output.consistency.level", ConsistencyLevel.ONE.toString)
        .setAppName(getClass.getSimpleName)

    val sc = new SparkContext(sparkMaster, "KafkaToDSE", sConf)

    val keyspace = "realtime"
    val table = "planes"
    CassandraConnector(sConf).withSessionDo {
      session =>
        session.execute(s"DROP KEYSPACE IF EXISTS $keyspace")
        session.execute(s"CREATE KEYSPACE IF NOT EXISTS $keyspace WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': $kReplicationFactor }")
        session.execute(s"DROP TABLE IF EXISTS $keyspace.$table")

        // FiXME: Dynamically create the CREATE TABLE sql based on schema
        session.execute(s"""
        CREATE TABLE IF NOT EXISTS $keyspace.$table
        (
          id text,
          ts timestamp,
          speed double,
          dist double,
          bearing double,
          rtid int,
          orig text,
          dest text,
          secstodep int,
          lon double,
          lat double,
          geometry text,

          PRIMARY KEY (id, ts)
        )"""
        )

        if (useSolr) {
          //
          // NOTE: LOOK AT THE SOFT COMMIT INTERVAL IN SOLR
          //
          // enable search on all fields (except geometry)
          session.execute(
            s"""
               | CREATE SEARCH INDEX ON $keyspace.$table
               | WITH COLUMNS
               |  id,
               |  ts,
               |  speed,
               |  dist,
               |  bearing,
               |  rtid,
               |  orig,
               |  dest,
               |  secstodep,
               |  lon,
               |  lat
         """.stripMargin
          )

          // check if we want to store the Geo
          if (storeGeo.toBoolean) {
            // enable search on geometry field
            session.execute(
              s"""
                 |ALTER SEARCH INDEX SCHEMA ON $keyspace.$table
                 |ADD types.fieldType[ @name='rpt',
                 |                     @class='solr.SpatialRecursivePrefixTreeFieldType',
                 |                     @geo='false',
                 |                     @worldBounds='ENVELOPE(-1000, 1000, 1000, -1000)',
                 |                     @maxDistErr='0.001',
                 |                     @distanceUnits='degrees' ]
           """.stripMargin
            )
            session.execute(
              s"""
                 |ALTER SEARCH INDEX SCHEMA ON $keyspace.$table
                 |ADD fields.field[ @name='geometry',
                 |                  @type='rpt',
                 |                  @indexed='true',
                 |                  @stored='true' ];
           """.stripMargin
            )
            session.execute(
              s"RELOAD SEARCH INDEX ON $keyspace.$table"
            )
          }
        }

    }

    // the streaming context
    val ssc = new StreamingContext(sc, Milliseconds(emitInterval.toInt))

    // create the kafka stream
    val stream = createKafkaStream(ssc, kBrokers, kConsumerGroup, kTopics, kThreads.toInt)

    // very specific adaptation for performance
    val dataStream = stream.map(line => adaptSpecific(line))

    // debug
    if (kDebug.toBoolean) {
      dataStream.foreachRDD {
        (rdd, time) =>
          val count = rdd.count()
          if (count > 0) {
            val msg = "Time %s: saving to DSE (%s total records)".format(time, count)
            log.warn(msg)
            println(msg)
          }
      }
    }

    // save to cassandra
    dataStream.foreachRDD {
      (rdd, _) =>
        rdd.saveToCassandra(
          keyspace,
          table,
          // FIXME: Do we need to specify all the columns?
          SomeColumns(
            "id",
            "ts",
            "speed",
            "dist",
            "bearing",
            "rtid",
            "orig",
            "dest",
            "secstodep",
            "lon",
            "lat",
            "geometry"
          )
        )
    }

    // start the stream
    ssc.start
    ssc.awaitTermination()
  }

  // create the kafka stream
  private def createKafkaStream(ssc: StreamingContext, brokers: String, consumerGroup: String, topics: String, numOfThreads: Int = 1): DStream[String] = {
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> brokers,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> consumerGroup,
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )
    val topicMap = topics.split(",")
    val kafkaStreams = (1 to numOfThreads).map { i =>
      KafkaUtils.createDirectStream[String, String](ssc, PreferConsistent, Subscribe[String, String](topicMap, kafkaParams)).map(_.value())
    }
    val unifiedStream = ssc.union(kafkaStreams)
    unifiedStream
  }

  // used to generate a random uuid
  private val RANDOM = new Random()

  // initialized the object mapper / text parser only once
  private val objectMapper = {
    // create an empty schema
    val schema = CsvSchema.emptySchema()
        .withColumnSeparator(',')
        .withLineSeparator("\\n")
    // create the mapper
    val csvMapper = new CsvMapper()
    csvMapper.enable(CsvParser.Feature.WRAP_AS_ARRAY)
    csvMapper
        .readerFor(classOf[Array[String]])
        .`with`(schema)
  }


  /**
    * Adapt to the very specific Safegraph Schema
    */
  private def adaptSpecific(line: String) = {
    val uuid = new UUID(RANDOM.nextLong(), RANDOM.nextLong())

    // parse out the line
    val rows = objectMapper.readValues[Array[String]](line)
    val row = rows.nextValue()

    val id = uuid.toString              // NOTE: This is to ensure unique records
    val ts = row(1).toLong
    val speed = row(2).toDouble
    val dist = row(3).toDouble
    val bearing = row(4).toDouble
    val rtid = row(5).toInt
    val orig = row(6)
    val dest = row(7)
    val secsToDep = row(8).toInt
    val longitude = row(9).toDouble
    val latitude = row(10).toDouble
    val geometryText = "POINT (" + row(9) + " " + row(10) + ")"

    // FIXME: why do we need to convert to tuples? why cant we store the data as a map?
    val data = (id, ts, speed, dist, bearing, rtid, orig, dest, secsToDep, latitude, longitude, geometryText)
    //println(data)
    data
  }
}
