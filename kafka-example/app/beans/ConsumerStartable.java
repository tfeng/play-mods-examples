/**
 * Copyright 2016 Thomas Feng
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
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import controllers.protocols.UserMessage;
import me.tfeng.toolbox.kafka.AvroDecoder;
import me.tfeng.toolbox.spring.ExtendedStartable;
import play.Logger;
import play.Logger.ALogger;
import utils.Constants;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component
public class ConsumerStartable implements ExtendedStartable {

  private static class ConsumerRunnable implements Runnable {

    private static final ALogger LOG = Logger.of(ConsumerRunnable.class);

    private final KafkaConsumer<String, UserMessage> consumer;

    public ConsumerRunnable(KafkaConsumer<String, UserMessage> consumer) {
      this.consumer = consumer;
    }

    @Override
    public void run() {
      while (true) {
        for (ConsumerRecord<String, UserMessage> record : consumer.poll(1000)) {
          LOG.info("Consuming message: " + record.value());
        }
      }
    }
  }

  @Autowired
  @Qualifier("kafka-example.consumer-properties")
  private Properties consumerProperties;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  @Override
  public void afterStart() {
    KafkaConsumer<String, UserMessage> consumer = new KafkaConsumer<String, UserMessage>(
        consumerProperties,
        new StringDeserializer(),
        new AvroDecoder<>(UserMessage.class));
    consumer.subscribe(Collections.singletonList(Constants.TOPIC));
    scheduler.execute(new ConsumerRunnable(consumer));
  }

  @Override
  public void afterStop() {
  }

  @Override
  public void beforeStart() {
  }

  @Override
  public void beforeStop() {
    scheduler.shutdown();
  }

  @Override
  public void onStart() throws Throwable {
  }

  @Override
  public void onStop() throws Throwable {
  }
}
