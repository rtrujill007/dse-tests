package com.esri.realtime.dse

import com.esri.realtime.dse.test.Defaults

object KafkaToDSETester extends App {

  // start the loader
  val kafkaBrokers = s"localhost:${Defaults.KafkaPort}"
  val topic = "planes"
  val dseHost = "127.0.0.1"
  val parameters = Array("local[8]", "1000", kafkaBrokers, "planes-group1", topic, "1", dseHost, "1", "false", "false")
  KafkaToDSE.main(parameters)

}
