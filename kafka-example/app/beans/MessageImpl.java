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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import controllers.protocols.Message;
import controllers.protocols.RequestHeader;
import controllers.protocols.UserMessage;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import play.Logger;
import play.Logger.ALogger;
import play.mvc.Controller;
import play.mvc.Http.Request;
import utils.Constants;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component
public class MessageImpl implements Message {

  private static final ALogger LOG = Logger.of(MessageImpl.class);

  @Autowired(required = false)
  @Qualifier("kafka-example.producer-properties")
  private Properties producerProperties;

  private Producer<String, UserMessage> producer;

  @Override
  public Void send(UserMessage message) {
    if (producer == null) {
      producer = new Producer<>(new ProducerConfig(producerProperties));
    }

    Request request = Controller.request();
    Map<String, List<String>> query = Maps.transformValues(request.queryString(), value -> Arrays.asList(value));
    RequestHeader header = RequestHeader.newBuilder()
        .setRemoteAddress(request.remoteAddress())
        .setHost(request.host())
        .setMethod(request.method())
        .setPath(request.path())
        .setQuery(query)
        .setSecure(request.secure())
        .setTimestamp(new Date().getTime())
        .build();
    message.setRequestHeader(header);

    LOG.info("Producing message: " + message);

    try {
      producer.send(new KeyedMessage<String, UserMessage>(Constants.TOPIC, message.getSubject(), message));
      return null;
    } catch (Exception e) {
      throw new RuntimeException("Unable to send Kafka event for message: " + message, e);
    }
  }
}
