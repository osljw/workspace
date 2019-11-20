package org.test

/*
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

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.extensions._
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.api.common.functions._;

/**
 * This example shows an implementation of WordCount with data from a text socket.
 * To run the example make sure that the service providing the text data is already up and running.
 *
 * To start an example socket text stream on your local machine run netcat from a command line,
 * where the parameter specifies the port number:
 *
 * {{{
 *   nc -lk 9999
 * }}}
 *
 * Usage:
 * {{{
 *   SocketTextStreamWordCount <hostname> <port> <output path>
 * }}}
 *
 * This example shows how to:
 *
 *   - use StreamExecutionEnvironment.socketTextStream
 *   - write a simple Flink Streaming program in scala.
 *   - write and use user-defined functions.
 */
object TextStream {

  // http://wuchong.me/blog/2018/11/07/use-flink-calculate-hot-items/
  def main(args: Array[String]): Unit = {
    // if (args.length != 2) {
    //   System.err.println("USAGE:\nSocketTextStreamWordCount <hostname> <port>")
    //   return
    // }

    // val hostName = args(0)
    // val port = args(1).toInt

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

    class UserBehavior(userId: Long, itemId: Long, categoryId: Int, behavior: String, timestamp: Long) {
      val getUserId = userId
      val getBehavior = behavior
      val getTimestamp = timestamp
  
      override def toString(): String = {
        //userId.toString + itemId.toString + behavior
        s"userId:$userId, itemId:$itemId, behavior:$behavior, timestamp:$timestamp"
      }
    }
    //class UserBehavior(userId: Long, itemId: Long, categoryId: Int, behavior: String, timestamp: Long)
    //val csvInput = env.readCsvFile[UserBehavior]("data/UserBehavior.csv") 
    val textInput = env.readTextFile("data/UserBehavior.csv") 
    //textInput.print()

    def parse(s: String): UserBehavior = {
      val arr = s.split(',')
      val ub = new UserBehavior(arr(0).toInt, arr(1).toInt, arr(2).toInt, arr(3), arr(4).toInt)
      ub
    }
    val csvInput:DataStream[UserBehavior] = textInput.map(parse(_))
    //csvInput.setParallelism(1).print()

    val timedData = csvInput.assignTimestampsAndWatermarks(new AscendingTimestampExtractor[UserBehavior]() {
      override def extractAscendingTimestamp(ub: UserBehavior):Long = {
        ub.getTimestamp * 1000
      }
    })

    val pvData = timedData.filter(new FilterFunction[UserBehavior]() {
      override def filter(ub: UserBehavior) = {
        ub.getBehavior == "cart"
      }
    })
    pvData.setParallelism(1).print()
    
    //print(csvInput)
    //textInput.print()


    //Create streams for names and ages by mapping the inputs to the corresponding objects
    // val text = env.socketTextStream(hostName, port)
    // val counts = text.flatMap { _.toLowerCase.split("\\W+") filter { _.nonEmpty } }
    //   .map { (_, 1) }
    //   .keyBy(0)
    //   .sum(1)

    // counts print

    env.execute("Scala SocketTextStreamWordCount Example")
  }

}
