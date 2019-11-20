package org.test

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Properties
import java.io.File
import com.typesafe.config.{ Config, ConfigFactory }

import scala.util.parsing.json._
import scala.collection.immutable.{Map}

//import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.scala._
import org.apache.flink.api.common.serialization.SimpleStringSchema;
//import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer08, FlinkKafkaProducer08}
import org.apache.flink.streaming.connectors.kafka.{FlinkKafkaConsumer, FlinkKafkaProducer}

/**
 * Skeleton for a Flink Job.
 *
 * For a full example of a Flink Job, see the WordCountJob.scala file in the
 * same package/directory or have a look at the website.
 *
 * You can also generate a .jar file that you can submit on your Flink
 * cluster. Just type
 * {{{
 *   sbt clean assembly
 * }}}
 * in the projects root directory. You will find the jar in
 * target/scala-2.11/Flink\ Project-assembly-0.1-SNAPSHOT.jar
 *
 */

class KeyedData(_hostname: String, _ts: String, _data: Map[String, String]) {
  val hostname: String = _hostname
  val ts: String = _ts
  var is_new:Boolean = false
  val data:Map[String, String] = _data

  def compare(other: KeyedData): KeyedData = {
    if (other.data("rank_cpa") != data("rank_cpa")) {
      is_new = true
    } else {
      is_new = false
    }

    return this
  }

  override def toString(): String = {
    return "hostname:" + hostname +
      ",ts:" + ts + 
      ",scheduling_id:" + data("scheduling_id") + 
      ",cpa:" + data("cpa") + 
      ",act_cpa:" + data("act_cpa") +
      ",rank_cpa:" + data("rank_cpa")
  }
}

object KafkaStream {

//   def regJson(json:Option[Any]) = json match {
//     case Some(s: String) => s  
//     case Some(map: Map[String, Any]) => map
// //      case None => "erro"
// //      case other => "Unknow data structure : " + other
//   }

  //def field_extract(line: String): Map[String, String] = {
  def field_extract(line: String): KeyedData = {
    var data_json = JSON.parseFull(line).get.asInstanceOf[Map[String, Any]]

    val field = data_json.get("fields").get.asInstanceOf[Map[String, Any]]
    val hostname = field.get("HOSTNAME").get.asInstanceOf[String]

    val log = data_json.get("body").get.asInstanceOf[String]
    val ts = log.substring(0, 23)

    val data = log.split(" ")
      .map(_.split(":"))
      .filter(_.size==2)
      .map{case Array(x,y) => (x,y)}
      .toMap
      .filterKeys(Set("scheduling_id", "cpa", "act_cpa", "rank_cpa"))
      //.filterKeys(Set("scheduling_id", "cpa", "act_cpa", "rank_cpa"))

    //print(data)

    val d = new KeyedData(hostname, ts, data)
    return d
  }



  def main(args: Array[String]): Unit = {
    // set up the execution environment
    //val env = ExecutionEnvironment.getExecutionEnvironment
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    /**
     * Here, you can start creating your execution plan for Flink.
     *
     * Start with getting some data from the environment, like
     * env.readTextFile(textPath);
     *
     * then, transform the resulting DataSet[String] using operations
     * like:
     *   .filter()
     *   .flatMap()
     *   .join()
     *   .group()
     *
     * and many more.
     * Have a look at the programming guide:
     *
     * http://flink.apache.org/docs/latest/programming_guide.html
     *
     * and the examples
     *
     * http://flink.apache.org/docs/latest/examples.html
     *
     */
    print("================ start =====================")
    // val config = ConfigFactory.parseFile(new File("conf/stream.conf"))
    // print("config" + config)
    // print("config:" + config.getString("bootstrap_servers"))

    val properties = new Properties()
    // only required for Kafka 0.8
    // properties.setProperty("zookeeper.connect", "ip1:2181,ip2:2181")
    properties.setProperty("bootstrap.servers", "ip1:9092,ip2:9092")
    properties.setProperty("group.id", "test123")

    // val stream = env.addSource(new FlinkKafkaConsumer08[String]("topic_name", new SimpleStringSchema(), properties))


    var stream = env.addSource(new FlinkKafkaConsumer[String]("topic_name", new SimpleStringSchema(), properties))

    var data_stream = stream.filter(str => (str contains "[QUERY_RESULT]") && (str contains "cast_way:2"))

    // var map_stream = data_stream.map(field_extract(_))
    //   //.toMap
    //   .keyBy(d => d.data("scheduling_id"))
    //   .reduce( (i1, i2) => i2)
    //   .map(d => d.filterKeys(Set("scheduling_id", "cpa", "rank_cpa")))
      
    var map_stream = data_stream.map(field_extract(_))
      .keyBy(d => (d.hostname, d.data("scheduling_id")))
      .reduce((i1, i2) => i2.compare(i1))
      .filter(d => d.is_new == true)

    map_stream.print()

    // execute program
    env.execute("Flink Scala API Skeleton")
  }

}
