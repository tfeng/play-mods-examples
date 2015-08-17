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
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IntegrationTest {

  private static final int PORT = 3333;

  private static final int TIMEOUT = Integer.MAX_VALUE;

  @Test
  public void testEmptyGet() {
    running(testServer(PORT), () -> {
      try {
        WSResponse response = WS.url("http://localhost:" + PORT + "/get").get().get(TIMEOUT);
        assertThat(response.getBody(), is("{}"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testMessages() {
    running(testServer(PORT), () -> {
      try {
        WS.url("http://localhost:" + PORT + "/add")
            .setQueryParameter("message", "hello world thomas")
            .get()
            .get(TIMEOUT);
        WS.url("http://localhost:" + PORT + "/add").setQueryParameter("message", "hello world amy").get().get(TIMEOUT);

        while (true) {
          WSResponse response = WS.url("http://localhost:" + PORT + "/get").get().get(TIMEOUT);
          ObjectNode json = (ObjectNode) Json.parse(response.getBody());
          int helloCount = getCount(json, "hello");
          int worldCount = getCount(json, "world");
          int thomasCount = getCount(json, "thomas");
          int amyCount = getCount(json, "amy");
          if (helloCount == 2 && worldCount == 2 && thomasCount == 1 && amyCount == 1) {
            break;
          } else {
            assertThat(helloCount, lessThanOrEqualTo(2));
            assertThat(worldCount, lessThanOrEqualTo(2));
            assertThat(thomasCount, lessThanOrEqualTo(1));
            assertThat(amyCount, lessThanOrEqualTo(1));
          }
          Thread.sleep(100);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private int getCount(ObjectNode json, String word) {
    JsonNode node = json.get(word);
    if (node == null) {
      return 0;
    } else {
      return node.numberValue().intValue();
    }
  }
}
