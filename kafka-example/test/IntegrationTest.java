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

import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Before;
import org.junit.Test;

import controllers.protocols.MessageClient;
import controllers.protocols.UserMessage;
import me.tfeng.playmods.avro.AvroComponent;
import me.tfeng.playmods.spring.ApplicationLoader;
import me.tfeng.toolbox.kafka.AvroDecoder;
import me.tfeng.toolbox.spring.ApplicationManager;
import play.Application;
import play.ApplicationLoader.Context;
import play.Environment;
import play.test.TestServer;
import utils.Constants;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IntegrationTest {

  private static final int PORT = 3333;

  private Application application;

  @Before
  public void setup() {
    application = new ApplicationLoader().load(new Context(Environment.simple()));
  }

  @Test
  public void testKafkaEvents() {
    TestServer server = testServer(PORT, application);
    running(server, () -> {
      try {
        Date beginning = new Date();

        Properties consumerProperties = new Properties();
        InputStream propertiesStream = getClass().getClassLoader().getResourceAsStream("test-consumer.properties");
        consumerProperties.load(propertiesStream);

        KafkaConsumer<String, UserMessage> consumer = new KafkaConsumer<String, UserMessage>(
            consumerProperties,
            new StringDeserializer(),
            new AvroDecoder<>(UserMessage.class));
        consumer.subscribe(Collections.singletonList(Constants.TOPIC));

        MessageClient client =
            getAvroComponent().client(MessageClient.class, new URL("http", "localhost", PORT, "/message"));
        UserMessage messageOut = UserMessage.newBuilder()
            .setSubject("Raymond")
            .setAction("reads")
            .setObject("books")
            .setRequestHeader(null)
            .build();
        client.send(messageOut).toCompletableFuture();

        ConsumerRecords<String, UserMessage> consumerRecords = consumer.poll(10000);
        assertFalse(consumerRecords.isEmpty());

        UserMessage messageIn = consumerRecords.iterator().next().value();
        assertEquals(messageIn.getSubject(), "Raymond");
        assertEquals(messageIn.getAction(), "reads");
        assertEquals(messageIn.getObject(), "books");
        assertEquals(messageIn.getRequestHeader().getRemoteAddress(), "127.0.0.1");
        assertEquals(messageIn.getRequestHeader().getHost(), "localhost:3333");
        assertEquals(messageIn.getRequestHeader().getMethod(), "POST");
        assertEquals(messageIn.getRequestHeader().getPath(), "/message");
        assertEquals(messageIn.getRequestHeader().getQuery(), Collections.emptyMap());
        assertFalse(messageIn.getRequestHeader().getSecure());
        assertThat(messageIn.getRequestHeader().getTimestamp(), greaterThan(beginning.getTime()));

        consumer.close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private AvroComponent getAvroComponent() {
    return application.injector().instanceOf(ApplicationManager.class).getBean(AvroComponent.class);
  }
}
