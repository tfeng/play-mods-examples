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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.ipc.Ipc;
import org.apache.avro.ipc.generic.GenericRequestor;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import controllers.protocols.Example;
import controllers.protocols.ExampleClient;
import controllers.protocols.KTooLargeError;
import controllers.protocols.Point;
import controllers.protocols.Points;
import controllers.protocols.PointsClient;
import me.tfeng.playmods.avro.AvroComponent;
import me.tfeng.playmods.modules.SpringModule;
import me.tfeng.toolbox.spring.ApplicationManager;
import play.libs.F.Promise;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IntegrationTest {

  private static final int TIMEOUT = Integer.MAX_VALUE;

  @Test
  public void testExampleBinaryRequest() {
    running(testServer(3333), () -> {
      AvroComponent avroComponent = SpringModule.getApplicationManager().getBean(AvroComponent.class);
      try {
        Example example =
            avroComponent.client(Example.class, new URL("http://localhost:3333/example"));
        assertThat(example.echo("Test Message"), is("Test Message"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testExampleBinaryRequestAsync() {
    running(testServer(3333), () -> {
      AvroComponent avroComponent = SpringModule.getApplicationManager().getBean(AvroComponent.class);
      try {
        ExampleClient example =
            avroComponent.client(ExampleClient.class, new URL("http://localhost:3333/example"));
        Promise<String> promise = example.echo("Test Message");
        assertThat(promise.get(TIMEOUT), is("Test Message"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testExampleJsonRequest() {
    running(testServer(3333), () -> {
      try {
        Object response =
            sendJsonRequest("http://localhost:3333/example", Example.PROTOCOL, "echo",
                "{\"message\": \"Test Message\"}");
        assertThat(response, is("Test Message"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testPointsBinaryRequest() {
    running(testServer(3333), () -> {
      AvroComponent avroComponent = SpringModule.getApplicationManager().getBean(AvroComponent.class);
      try {
        Points points = avroComponent.client(Points.class, new URL("http://localhost:3333/points"));
        Point center = Point.newBuilder().setX(0.0).setY(0.0).build();

        // []
        try {
          points.getNearestPoints(center, 1);
          fail("KTooLargeError is expected");
        } catch (KTooLargeError e) {
          assertThat(e.getK(), is(1));
        }

        // [one]
        Point one = Point.newBuilder().setX(1.0).setY(1.0).build();
        points.addPoint(one);
        assertThat(points.getNearestPoints(center, 1), is(ImmutableList.of(one)));
        try {
          points.getNearestPoints(center, 2);
          fail("KTooLargeError is expected");
        } catch (KTooLargeError e) {
          assertThat(e.getK(), is(2));
        }

        // [one, five]
        Point five = Point.newBuilder().setX(5.0).setY(5.0).build();
        points.addPoint(five);
        assertThat(points.getNearestPoints(center, 1), is(ImmutableList.of(one)));
        assertThat(points.getNearestPoints(center, 2), is(ImmutableList.of(one, five)));
        try {
          points.getNearestPoints(center, 3);
          fail("KTooLargeError is expected");
        } catch (KTooLargeError e) {
          assertThat(e.getK(), is(3));
        }

        // [one, five, five]
        points.addPoint(five);
        assertThat(points.getNearestPoints(center, 1), is(ImmutableList.of(one)));
        assertThat(points.getNearestPoints(center, 2), is(ImmutableList.of(one, five)));
        assertThat(points.getNearestPoints(center, 3), is(ImmutableList.of(one, five, five)));
        try {
          points.getNearestPoints(center, 4);
          fail("KTooLargeError is expected");
        } catch (KTooLargeError e) {
          assertThat(e.getK(), is(4));
        }

        // []
        points.clear();
        try {
          points.getNearestPoints(center, 1);
          fail("KTooLargeError is expected");
        } catch (KTooLargeError e) {
          assertThat(e.getK(), is(1));
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testPointsBinaryRequestAsync() {
    running(testServer(3333), () -> {
      AvroComponent avroComponent = SpringModule.getApplicationManager().getBean(AvroComponent.class);
      try {
        PointsClient points =
            avroComponent.client(PointsClient.class, new URL("http://localhost:3333/points"));
        Point center = Point.newBuilder().setX(0.0).setY(0.0).build();

        // []
        points.getNearestPoints(center, 1)
            .map(response -> {
              fail("KTooLargeError is expected");
              return null;
            })
            .recover(error -> {
              assertThat(error, instanceOf(KTooLargeError.class));
              assertThat(((KTooLargeError) error).getK(), is(1));
              return null;
            })
            .get(TIMEOUT);

        // [one]
        Point one = Point.newBuilder().setX(1.0).setY(1.0).build();
        points.addPoint(one).get(TIMEOUT);
        points.getNearestPoints(center, 1)
            .map(response -> {
              assertThat(response, is(ImmutableList.of(one)));
              return null;
            }).get(TIMEOUT);
        points.getNearestPoints(center, 2)
            .map(response -> {
              fail("KTooLargeError is expected");
              return null;
            })
            .recover(error -> {
              assertThat(error, instanceOf(KTooLargeError.class));
              assertThat(((KTooLargeError) error).getK(), is(2));
              return null;
            }).get(TIMEOUT);

        // [one, five]
        Point five = Point.newBuilder().setX(5.0).setY(5.0).build();
        points.addPoint(five).get(TIMEOUT);
        points.getNearestPoints(center, 1)
            .map(response -> {
              assertThat(response, is(ImmutableList.of(one)));
              return null;
            }).get(TIMEOUT);
        points.getNearestPoints(center, 2)
            .map(response -> {
              assertThat(response, is(ImmutableList.of(one, five)));
              return null;
            }).get(TIMEOUT);
        points.getNearestPoints(center, 3)
            .map(response -> {
              fail("KTooLargeError is expected");
              return null;
            })
            .recover(error -> {
              assertThat(error, instanceOf(KTooLargeError.class));
              assertThat(((KTooLargeError) error).getK(), is(3));
              return null;
            }).get(TIMEOUT);

        // [one, five, five]
        points.addPoint(five).get(TIMEOUT);
        points.getNearestPoints(center, 1)
            .map(response -> {
              assertThat(response, is(ImmutableList.of(one)));
              return null;
            }).get(TIMEOUT);
        points.getNearestPoints(center, 2)
            .map(response -> {
              assertThat(response, is(ImmutableList.of(one, five)));
              return null;
            }).get(TIMEOUT);
        points.getNearestPoints(center, 3)
            .map(response -> {
              assertThat(response, is(ImmutableList.of(one, five, five)));
              return null;
            }).get(TIMEOUT);
        points.getNearestPoints(center, 4)
            .map(response -> {
              fail("KTooLargeError is expected");
              return null;
            })
            .recover(error -> {
              assertThat(error, instanceOf(KTooLargeError.class));
              assertThat(((KTooLargeError) error).getK(), is(4));
              return null;
            }).get(TIMEOUT);

        // []
        points.clear().get(TIMEOUT);
        points.getNearestPoints(center, 1)
            .map(response -> {
              fail("KTooLargeError is expected");
              return null;
            })
            .recover(error -> {
              assertThat(error, instanceOf(KTooLargeError.class));
              assertThat(((KTooLargeError) error).getK(), is(1));
              return null;
            }).get(TIMEOUT);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testPointsJsonRequest() {
    running(testServer(3333), () -> {
      try {
        String url = "http://localhost:3333/points";
        GenericData.Record record = new GenericData.Record(Points.PROTOCOL.getMessages()
            .get("getNearestPoints").getErrors().getTypes().get(1));
        Object response;

        // []
        try {
          response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
              "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
          fail("AvroRemoteException is expected");
        } catch (AvroRemoteException e) {
          record.put("k", 1);
          assertThat(e.getValue(), is(record));
        }

        // [one]
        Point one = Point.newBuilder().setX(1.0).setY(1.0).build();
        sendJsonRequest(url, Points.PROTOCOL, "addPoint", "{\"point\": {\"x\": 1.0, \"y\": 1.0}}");
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
        assertThat(response.toString(), is(ImmutableList.of(one).toString()));
        try {
          response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
              "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 2}");
          fail("AvroRemoteException is expected");
        } catch (AvroRemoteException e) {
          record.put("k", 2);
          assertThat(e.getValue(), is(record));
        }

        // [one, five]
        Point five = Point.newBuilder().setX(5.0).setY(5.0).build();
        sendJsonRequest(url, Points.PROTOCOL, "addPoint", "{\"point\": {\"x\": 5.0, \"y\": 5.0}}");
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
        assertThat(response.toString(), is(ImmutableList.of(one).toString()));
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 2}");
        assertThat(response.toString(), is(ImmutableList.of(one, five).toString()));
        try {
          response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
              "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 3}");
          fail("AvroRemoteException is expected");
        } catch (AvroRemoteException e) {
          record.put("k", 3);
          assertThat(e.getValue(), is(record));
        }

        // [one, five, five]
        sendJsonRequest(url, Points.PROTOCOL, "addPoint", "{\"point\": {\"x\": 5.0, \"y\": 5.0}}");
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
        assertThat(response.toString(), is(ImmutableList.of(one).toString()));
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 2}");
        assertThat(response.toString(), is(ImmutableList.of(one, five).toString()));
        response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
            "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 3}");
        assertThat(response.toString(), is(ImmutableList.of(one, five, five).toString()));
        try {
          response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
              "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 4}");
          fail("AvroRemoteException is expected");
        } catch (AvroRemoteException e) {
          record.put("k", 4);
          assertThat(e.getValue(), is(record));
        }

        // []
        sendJsonRequest(url, Points.PROTOCOL, "clear", "");
        try {
          response = sendJsonRequest(url, Points.PROTOCOL, "getNearestPoints",
              "{\"from\": {\"x\": 0.0, \"y\": 0.0}, \"k\": 1}");
          fail("AvroRemoteException is expected");
        } catch (AvroRemoteException e) {
          record.put("k", 1);
          assertThat(e.getValue(), is(record));
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private Object sendJsonRequest(String url, Protocol protocol, String message, String data)
      throws URISyntaxException, IOException {
    URI uri = new URL(url).toURI();
    Schema schema = protocol.getMessages().get(message).getRequest();
    GenericRequestor client = new GenericRequestor(protocol, Ipc.createTransceiver(uri));
    GenericDatumReader<Object> reader = new GenericDatumReader<>(schema);
    Object request = reader.read(null, DecoderFactory.get().jsonDecoder(schema, data));
    return client.request(message, request);
  }
}
