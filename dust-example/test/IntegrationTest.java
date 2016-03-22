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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.junit.Before;
import org.junit.Test;

import me.tfeng.playmods.spring.ApplicationLoader;
import me.tfeng.playmods.spring.ExceptionWrapper;
import play.Application;
import play.ApplicationLoader.Context;
import play.Environment;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

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
  public void testCustomName() {
    running(testServer(PORT, application), () -> {
      WSClient client = WS.newClient(PORT);
      WSRequest request = client.url("/").setQueryParameter("name", "Amy");
      WSResponse response = ExceptionWrapper.wrap(() -> request.get().toCompletableFuture().get());
      assertThat(response.getBody(), is("Hello, Amy!"));
    });
  }

  @Test
  public void testDefaultName() {
    running(testServer(PORT, application), () -> {
      WSClient client = WS.newClient(PORT);
      WSRequest request = client.url("/");
      WSResponse response = ExceptionWrapper.wrap(() -> request.get().toCompletableFuture().get());
      assertThat(response.getBody(), is("Hello, Thomas!"));
    });
  }
}
