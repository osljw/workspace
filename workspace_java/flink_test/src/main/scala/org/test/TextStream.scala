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
import org.apache.flink.streaming.api.scala.function.{ProcessWindowFunction, WindowFunction}
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple1;

import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.scala._

import scala.collection.mutable.ArrayBuffer
import java.sql.Timestamp
import scala.collection.JavaConversions._  
import scala.math.min

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

case class UserBehavior(userId: Long, itemId: Long, categoryId: Int, behavior: String, timestamp: Long) {
  override def toString: String = {
    //userId.toString + itemId.toString + behavior
    s"userId:$userId, itemId:$itemId, behavior:$behavior, timestamp:$timestamp"
  }
}

class CountAgg extends AggregateFunction[UserBehavior, Long, Long] {
  override def createAccumulator(): Long = {
    0L
  }

  override def add(ub: UserBehavior, acc: Long): Long = {
    acc + 1
  }

  override def getResult(acc: Long): Long = {
    acc
  }

  override def merge(acc1: Long, acc2: Long): Long = {
    acc1 + acc2
  }
}

case class ItemViewCount(itemId: Long, windowEnd: Long, viewCount: Long) {
  override def toString: String = {
    s"itemId:$itemId, windowEnd:$windowEnd, count:$viewCount"
  }
}

// class WindowResultFunction extends WindowFunction[Long, ItemViewCount, Tuple, TimeWindow] {
//   override def apply(key: Tuple, window: TimeWindow, aggregateResult: Iterable[Long], collector: Collector[ItemViewCount]) = {
//     val itemId: Long = key.asInstanceOf[Tuple1[Long]].f0
//     val count: Long = aggregateResult.iterator.next()
//     collector.collect(ItemViewCount(itemId, window.getEnd(), count))
//   }
// }

class WindowResultFunction extends ProcessWindowFunction[Long, ItemViewCount, Tuple, TimeWindow] {
  override def process(key: Tuple, context: Context, aggregateResult: Iterable[Long], collector: Collector[ItemViewCount]) = {
    val itemId: Long = key.asInstanceOf[Tuple1[Long]].f0
    val count: Long = aggregateResult.iterator.next()
    collector.collect(ItemViewCount(itemId, context.window.getEnd(), count))
  }
}

// KeyedProcessFunction[K, I, O]
class TopNHotItems(val topSize: Int) extends KeyedProcessFunction[Tuple, ItemViewCount, String] {
  //private var itemState: ListState[ItemViewCount]
  private var itemState: ListState[ItemViewCount] = _

  override def open(parameters: Configuration) = {
    super.open(parameters)
    val itemsStateDesc: ListStateDescriptor[ItemViewCount] = new ListStateDescriptor[ItemViewCount](
      "itemState-state",
      createTypeInformation[ItemViewCount]) 
    itemState = getRuntimeContext().getListState(itemsStateDesc) 
  }

  override def processElement(input: ItemViewCount, 
      context: KeyedProcessFunction[Tuple, ItemViewCount, String]#Context, 
      out: Collector[String]): Unit = {
    itemState.add(input)
    context.timerService().registerEventTimeTimer(input.windowEnd + 1);
  } 

  override def onTimer(timestamp: Long, 
      ctx: KeyedProcessFunction[Tuple, ItemViewCount, String]#OnTimerContext,
      out: Collector[String]): Unit = {

    var allItems = new ArrayBuffer[ItemViewCount]();
    for (i <- itemState.get()) {
      allItems += i;
    }
    itemState.clear();
    allItems = allItems.sortBy(x => -x.viewCount)


    val result = new StringBuilder
    result.append("======================\n")
    result.append("time:").append(new Timestamp(timestamp - 1))
      .append("-").append(timestamp - 1).append("\n")
    for (i <- 0 to min(topSize, allItems.length) - 1) {
      val currentItem: ItemViewCount = allItems(i);
      result.append("No:").append(i) 
        .append("ID:").append(currentItem.itemId)
        .append("购买量：").append(currentItem.viewCount).append("\n")
    }
    result.append("======================\n")

    out.collect(result.toString)
  }
}

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
    env.setParallelism(1);

    // class UserBehavior(userId: Long, itemId: Long, categoryId: Int, behavior: String, timestamp: Long) {
    //   val getUserId = userId
    //   val getBehavior = behavior
    //   val getTimestamp = timestamp
  
    //   override def toString(): String = {
    //     //userId.toString + itemId.toString + behavior
    //     s"userId:$userId, itemId:$itemId, behavior:$behavior, timestamp:$timestamp"
    //   }
    // }


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
        //ub.getTimestamp * 1000
        ub.timestamp * 1000
      }
    })

    val cartData = timedData.filter(new FilterFunction[UserBehavior]() {
      override def filter(ub: UserBehavior) = {
        //ub.getBehavior == "cart"
        ub.behavior == "cart"
      }
    })
    //pvData.setParallelism(1).print()

    // 每隔5分钟输出最近一个小时每个商品的购买量
    val windowedData = cartData.keyBy("itemId")
      .timeWindow(Time.minutes(60), Time.minutes(5))
      .aggregate(new CountAgg(), new WindowResultFunction())
    //windowedData.print()

    val topItems = windowedData.keyBy("windowEnd")
      .process(new TopNHotItems(3))
    topItems.print()

    

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
