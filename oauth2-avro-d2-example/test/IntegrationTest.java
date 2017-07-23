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

import java.io.ByteArrayInputStream;
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

import akka.stream.javadsl.StreamConverters;
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
import play.libs.ws.SourceBodyWritable;
import play.libs.ws.StandaloneWSResponse;
import play.libs.ws.ahc.StandaloneAhcWSClient;
import play.libs.ws.ahc.StandaloneAhcWSRequest;

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
        builder.addHeader("Authorization", "Bearer " + authorizationToken);
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
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response;

      response = authenticateClient(client, TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = Json.parse(response.getBody()).findPath("accessToken").textValue();

      response = authenticateUser(client, clientAccessToken, "test", USER_PASSWORD);
      String userAccessToken = Json.parse(response.getBody()).findPath("accessToken").textValue();

      response = get(client, "/proxy", "Authorization", "Bearer " + userAccessToken, "message",
          "Test Message through Client");
      assertThat(response.getBody(), is("Test Message through Client"));
    }));
  }

  @Test
  public void testD2RequestMissingAuthorization() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response = get(client, "/proxy", "message", "Test Message through Client");
      assertThat(response.getStatus(), is(401));
    }));
  }

  @Test
  public void testD2RequestWrongAuthorization() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response;

      response = authenticateClient(client, TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = Json.parse(response.getBody()).findPath("accessToken").textValue();

      response = get(client, "/proxy", "Authorization", "Bearer " + clientAccessToken, "message",
          "Test Message through Client");
      assertThat(response.getStatus(), is(401));
    }));
  }

  @Test
  public void testDirectRequest() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response;

      response = authenticateClient(client, TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = Json.parse(response.getBody()).findPath("accessToken").textValue();

      response = authenticateUser(client, clientAccessToken, "test", USER_PASSWORD);
      String userAccessToken = Json.parse(response.getBody()).findPath("accessToken").textValue();

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
        StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
        StandaloneWSResponse response = authenticateClient(client, TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
        String clientAccessToken = Json.parse(response.getBody()).findPath("accessToken").textValue();

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
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response;

      response = authenticateClient(client, UNTRUSTED_CLIENT_ID, UNTRUSTED_CLIENT_SECRET);
      String clientAccessToken = Json.parse(response.getBody()).findPath("accessToken").textValue();

      response = authenticateUser(client, clientAccessToken, "test", USER_PASSWORD);
      assertThat(response.getStatus(), is(401));
    }));
  }

  private StandaloneWSResponse authenticateClient(StandaloneAhcWSClient client, String clientId, String clientSecret)
      throws ExecutionException, InterruptedException {
    ObjectNode body = Json.newObject();
    body.setAll(ImmutableMap.of("clientId", body.textNode(clientId), "clientSecret", body.textNode(clientSecret)));
    return post(client, "/client/authenticate", body.toString());
  }

  private StandaloneWSResponse authenticateUser(StandaloneAhcWSClient client, String clientAccessToken, String username,
      String password) throws ExecutionException, InterruptedException {
    ObjectNode body = Json.newObject();
    body.setAll(ImmutableMap.of("username", body.textNode(username), "password", body.textNode(password)));
    return post(client, "/user/authenticate", "Authorization", "Bearer " + clientAccessToken, body.toString());
  }

  private StandaloneWSResponse get(StandaloneAhcWSClient client, String endpoint, String queryParameterName,
      String queryParameterValue) throws ExecutionException, InterruptedException {
    return get(client, endpoint, null, null, queryParameterName, queryParameterValue);
  }

  private StandaloneWSResponse get(StandaloneAhcWSClient client, String endpoint, String headerName, String headerValue,
      String queryParameterName, String queryParameterValue) throws ExecutionException, InterruptedException {
    StandaloneAhcWSRequest request = client.url("http://localhost:" + PORT + endpoint);
    if (headerName != null) {
      request = request.addHeader(headerName, headerValue);
    }
    return request.addQueryParameter(queryParameterName, queryParameterValue).get().toCompletableFuture().get();
  }

  private AvroComponent getAvroComponent() {
    return application.injector().instanceOf(ApplicationManager.class).getBean(AvroComponent.class);
  }

  private HttpRequestPoster getRequestPoster() {
    return application.injector().instanceOf(ApplicationManager.class).getBean(HttpRequestPoster.class);
  }

  private StandaloneWSResponse post(StandaloneAhcWSClient client, String endpoint, String data)
      throws ExecutionException, InterruptedException {
    return post(client, endpoint, null, null, data);
  }

  private StandaloneWSResponse post(StandaloneAhcWSClient client, String endpoint, String headerName,
      String headerValue, String data) throws ExecutionException, InterruptedException {
    StandaloneAhcWSRequest request = client.url("http://localhost:" + PORT + endpoint).setContentType("application/json");
    if (headerName != null) {
      request = request.addHeader(headerName, headerValue);
    }
    SourceBodyWritable bodyWritable = new SourceBodyWritable(
        StreamConverters.fromInputStream(() -> new ByteArrayInputStream(data.getBytes())));
    return request.post(bodyWritable).toCompletableFuture().get();
  }
}
