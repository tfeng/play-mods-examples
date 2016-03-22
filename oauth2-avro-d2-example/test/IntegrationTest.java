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
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.apache.avro.ipc.AsyncHttpTransceiver;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import controllers.protocols.Example;
import controllers.protocols.ExampleClient;
import me.tfeng.playmods.avro.AvroComponent;
import me.tfeng.playmods.http.HttpRequestPoster;
import me.tfeng.playmods.http.RequestPreparer;
import me.tfeng.playmods.oauth2.ApplicationLoader;
import me.tfeng.playmods.spring.ApplicationError;
import me.tfeng.playmods.spring.ExceptionWrapper;
import me.tfeng.toolbox.spring.ApplicationManager;
import play.Application;
import play.ApplicationLoader.Context;
import play.Environment;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IntegrationTest {

  private class TransceiverWithAuthorization extends AsyncHttpTransceiver {

    private final String authorizationToken;

    public TransceiverWithAuthorization(URL url, String authorizationToken) {
      super(url, getAvroComponent().getExecutor(), getRequestPoster());
      this.authorizationToken = authorizationToken;
    }

    @Override
    public CompletionStage<List<ByteBuffer>> transceive(List<ByteBuffer> request, RequestPreparer requestPreparer) {
      return super.transceive(request, (builder, contentType, url) -> {
        if (requestPreparer != null) {
          requestPreparer.prepare(builder, contentType, url);
        }
        builder.setHeader("Authorization", "Bearer " + authorizationToken);
      });
    }
  }

  private static final int PORT = 9000;

  private static final String TRUSTED_CLIENT_ID = "trusted-client";

  private static final String TRUSTED_CLIENT_SECRET = "trusted-client-password";

  private static final String UNTRUSTED_CLIENT_ID = "untrusted-client";

  private static final String UNTRUSTED_CLIENT_SECRET = "untrusted-client-password";

  private static final String USER_PASSWORD = "password";

  private Application application;

  @Before
  public void setup() {
    application = new ApplicationLoader().load(new Context(Environment.simple()));
  }

  @Test
  public void testD2Request() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      WSResponse response;
      WSClient client = WS.newClient(PORT);

      response = authenticateClient(TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = authenticateUser(clientAccessToken, "test", USER_PASSWORD);
      String userAccessToken = response.asJson().findPath("accessToken").textValue();

      response = client.url("/proxy")
          .setQueryParameter("message", "Test Message through Client")
          .setHeader("Authorization", "Bearer " + userAccessToken)
          .get()
          .toCompletableFuture()
          .get();
      assertThat(response.getBody(), is("Test Message through Client"));
    }));
  }

  @Test
  public void testD2RequestMissingAuthorization() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      WSClient client = WS.newClient(PORT);
      WSResponse response = client.url("/proxy")
          .setQueryParameter("message", "Test Message through Client")
          .get()
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(401));
    }));
  }

  @Test
  public void testD2RequestWrongAuthorization() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      WSResponse response;
      WSClient client = WS.newClient(PORT);

      response = authenticateClient(TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = client.url("/proxy")
          .setQueryParameter("message", "Test Message through Client")
          .setHeader("Authorization", "Bearer " + clientAccessToken)
          .get()
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(401));
    }));
  }

  @Test
  public void testDirectRequest() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      WSResponse response;

      response = authenticateClient(TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = authenticateUser(clientAccessToken, "test", USER_PASSWORD);
      String userAccessToken = response.asJson().findPath("accessToken").textValue();

      TransceiverWithAuthorization transceiver =
          new TransceiverWithAuthorization(new URL("http", "localhost", PORT, "/example"), userAccessToken);
      Example example = getAvroComponent().client(Example.class, transceiver);
      assertThat(example.echo("Test Message"), is("Test Message"));

      ExampleClient exampleClient = getAvroComponent().client(ExampleClient.class, transceiver);
      assertThat(exampleClient.echo("Test Message").toCompletableFuture().get(), is("Test Message"));
    }));
  }

  @Test
  public void testDirectRequestMissingAuthorization() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      try {
        URL url = new URL("http", "localhost", PORT, "/example");
        Example example = getAvroComponent().client(Example.class, url);
        example.echo("Test Message");
        fail("Exception is expected");
      } catch (Throwable t) {
        assertThat(t, instanceOf(ApplicationError.class));
        assertThat(t.getMessage(),
            is("Remote server at http://localhost:" + PORT + "/example returned HTTP response code 401"));
      }
    }));
  }

  @Test
  public void testDirectRequestWrongAuthorization() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      try {
        WSResponse response = authenticateClient(TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
        String clientAccessToken = response.asJson().findPath("accessToken").textValue();

        TransceiverWithAuthorization transceiver =
            new TransceiverWithAuthorization(new URL("http", "localhost", PORT, "/example"), clientAccessToken);
        Example example = getAvroComponent().client(Example.class, transceiver);
        example.echo("Test Message");
        fail("Exception is expected");
      } catch (Throwable t) {
        assertThat(t, instanceOf(ApplicationError.class));
        assertThat(ExceptionWrapper.unwrap(t).getMessage(),
            is("Remote server at http://localhost:" + PORT + "/example returned HTTP response code 401"));
      }
    }));
  }

  @Test
  public void testUserAccessDeniedWithUntrustedClient() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      WSResponse response;

      response = authenticateClient(UNTRUSTED_CLIENT_ID, UNTRUSTED_CLIENT_SECRET);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = authenticateUser(clientAccessToken, "test", USER_PASSWORD);
      assertThat(response.getStatus(), is(401));
    }));
  }

  private WSResponse authenticateClient(String clientId, String clientSecret) throws ExecutionException,
      InterruptedException {
    ObjectNode body = Json.newObject();
    body.setAll(ImmutableMap.of("clientId", body.textNode(clientId), "clientSecret", body.textNode(clientSecret)));
    WSClient client = WS.newClient(PORT);
    return client.url("/client/authenticate").post(body).toCompletableFuture().get();
  }

  private WSResponse authenticateUser(String clientAccessToken, String username, String password)
      throws ExecutionException, InterruptedException {
    ObjectNode body = Json.newObject();
    body.setAll(ImmutableMap.of("username", body.textNode(username), "password", body.textNode(password)));
    WSClient client = WS.newClient(PORT);
    return client.url("/user/authenticate")
        .setHeader("Authorization", "Bearer " + clientAccessToken)
        .post(body)
        .toCompletableFuture()
        .get();
  }

  private AvroComponent getAvroComponent() {
    return application.injector().instanceOf(ApplicationManager.class).getBean(AvroComponent.class);
  }

  private HttpRequestPoster getRequestPoster() {
    return application.injector().instanceOf(ApplicationManager.class).getBean(HttpRequestPoster.class);
  }
}
