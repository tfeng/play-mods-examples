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

package controllers;

import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;

import beans.SparkAppStartable;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Service
public class Application extends Controller {

  private static Producer<String, String> producer;

  @Autowired
  private SparkAppStartable job;

  @Value("${kafka.topic}")
  private String kafkaTopic;

  @Autowired(required = false)
  @Qualifier("spark-example.producer-properties")
  private Properties producerProperties;

  public Result addMessage(String message) throws Exception {
    if (producer == null) {
      producer = new Producer<>(new ProducerConfig(producerProperties));
    }
    producer.send(new KeyedMessage<>(kafkaTopic, kafkaTopic, message));
    return Results.ok();
  }

  public Result getValues() throws Exception {
    Map<String, Long> result = job.getCurrentValues();
    ObjectNode json = Json.newObject();
    result.forEach((k, v) -> json.put(k, v));
    return Results.ok(json);
  }
}
