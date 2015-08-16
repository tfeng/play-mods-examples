/**
 * Copyright 2015 Thomas Feng
 *
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

package beans;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.spark.Accumulable;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import me.tfeng.toolbox.spring.Startable;
import scala.Tuple2;
import utils.AccumulableLongMap;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("sparkAppStartable")
@DependsOn({"kafkaServerStartable", "sparkMasterStartable", "sparkWorkerStartable"})
public class SparkAppStartable implements Startable {

  private Accumulable<Map<String, Long>, Entry<String, Long>> accumulator;

  @Value("${kafka.group.id}")
  private String kafkaGroupId;

  @Value("${kafka.topic}")
  private String kafkaTopic;

  @Autowired
  private SparkConf sparkConf;

  private JavaStreamingContext streamingContext;

  @Value("${zookeeper.connect}")
  private String zkConnect;

  public Map<String, Long> getCurrentValues() {
    return accumulator.value();
  }

  @Override
  public void onStart() throws Throwable {
    JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);
    streamingContext = new JavaStreamingContext(sparkContext, Durations.seconds(1));
    Accumulable<Map<String, Long>, Entry<String, Long>> accumulator =
        sparkContext.accumulable(Maps.newHashMap(), new AccumulableLongMap());

    Map<String, Integer> topicMap = Collections.singletonMap(kafkaTopic, 1);
    JavaPairReceiverInputDStream<String, String> stream = KafkaUtils.createStream(streamingContext,
        zkConnect, kafkaGroupId, topicMap);
    JavaDStream<String> words = stream.flatMap(s -> Lists.newArrayList(s._2().split(" ")));
    JavaPairDStream<String, Long> wordCounts =
        words.mapToPair(s -> new Tuple2<>(s, 1l)).reduceByKey((i1, i2) -> i1 + i2);
    wordCounts.foreachRDD(rdd -> {
      rdd.foreach(tuple -> accumulator.add(new ImmutablePair<>(tuple._1(), tuple._2())));
      return null;
    });

    this.accumulator = accumulator;

    streamingContext.start();
  }

  @Override
  public void onStop() throws Throwable {
    streamingContext.stop();

  }
}
