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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import org.junit.Test;

import controllers.protocols.MessageClient;
import controllers.protocols.UserMessage;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.serializer.StringDecoder;
import me.tfeng.playmods.avro.AvroComponent;
import me.tfeng.playmods.kafka.AvroDecoder;
import me.tfeng.playmods.modules.SpringModule;
import utils.Constants;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IntegrationTest {

  private static final int TIMEOUT = Integer.MAX_VALUE;

  @Test
  public void testKafkaEvents() {
    running(testServer(3333), () -> {
      try {
        Date beginning = new Date();

        Properties consumerProperties = new Properties();
        InputStream propertiesStream =
            getClass().getClassLoader().getResourceAsStream("test-consumer.properties");
        consumerProperties.load(propertiesStream);

        ConsumerConnector consumer = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
        KafkaStream<String, UserMessage> stream = consumer.createMessageStreams(
            Collections.singletonMap(Constants.TOPIC, 1), new StringDecoder(null),
            new AvroDecoder<>(UserMessage.class)).get(Constants.TOPIC).get(0);

        MessageClient client = getAvroComponent()
            .client(MessageClient.class, new URL("http://localhost:3333/message"));
        client.send(
            UserMessage.newBuilder().setSubject("Raymond").setAction("reads").setObject("books")
                .setRequestHeader(null).build()).get(TIMEOUT);

        ConsumerIterator<String, UserMessage> iterator = stream.iterator();
        assertThat(iterator.hasNext(), is(true));

        UserMessage message = iterator.next().message();
        assertThat(message.getSubject(), is("Raymond"));
        assertThat(message.getAction(), is("reads"));
        assertThat(message.getObject(), is("books"));
        assertThat(message.getRequestHeader().getRemoteAddress(), is("127.0.0.1"));
        assertThat(message.getRequestHeader().getHost(), is("localhost:3333"));
        assertThat(message.getRequestHeader().getMethod(), is("POST"));
        assertThat(message.getRequestHeader().getPath(), is("/message"));
        assertThat(message.getRequestHeader().getQuery(), is(Collections.emptyMap()));
        assertThat(message.getRequestHeader().getSecure(), is(false));
        assertThat(message.getRequestHeader().getTimestamp(), greaterThan(beginning.getTime()));

        consumer.shutdown();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private AvroComponent getAvroComponent() {
    return SpringModule.getApplicationManager().getBean(AvroComponent.class);
  }
}
