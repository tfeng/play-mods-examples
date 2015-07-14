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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.ipc.AsyncHttpTransceiver;
import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import controllers.protocols.Example;
import controllers.protocols.ExampleClient;
import me.tfeng.playmods.avro.AvroComponent;
import me.tfeng.playmods.http.HttpRequestPoster;
import me.tfeng.playmods.http.RequestPreparer;
import me.tfeng.playmods.spring.ApplicationManager;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IntegrationTest {

  private class TransceiverWithAuthorization extends AsyncHttpTransceiver {

    private final String authorizationToken;

    public TransceiverWithAuthorization(URL url, String authorizationToken) {
      super(url, getAvroComponent().getExecutionContext(), getRequestPoster());
      this.authorizationToken = authorizationToken;
    }

    @Override
    public Promise<List<ByteBuffer>> transceive(List<ByteBuffer> request,
        RequestPreparer requestPreparer) {
      return super.transceive(request, (builder, contentType, url) -> {
        if (requestPreparer != null) {
          requestPreparer.prepare(builder, contentType, url);
        }
        builder.setHeader("Authorization", "Bearer " + authorizationToken);
      });
    }
  }

  private static final int PORT = 9000;

  private static final int TIMEOUT = Integer.MAX_VALUE;

  private static final String TRUSTED_CLIENT_ID = "trusted-client";

  private static final String TRUSTED_CLIENT_SECRET = "trusted-client-password";

  private static final String UNTRUSTED_CLIENT_ID = "untrusted-client";

  private static final String UNTRUSTED_CLIENT_SECRET = "untrusted-client-password";

  private static final String USER_PASSWORD = "password";

  @Test
  public void testD2Request() {
    running(testServer(PORT), () -> {
      try {
        WSResponse response;

        response = authenticateClient(TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
        String clientAccessToken = response.asJson().findPath("accessToken").textValue();

        response = authenticateUser(clientAccessToken, "test", USER_PASSWORD);
        String userAccessToken = response.asJson().findPath("accessToken").textValue();

        response = WS.url("http://localhost:" + PORT + "/proxy")
            .setQueryParameter("message", "Test Message through Client")
            .setHeader("Authorization", "Bearer " + userAccessToken).get().get(TIMEOUT);
        assertThat(response.getBody(), is("Test Message through Client"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testD2RequestMissingAuthorization() {
    running(testServer(PORT), () -> {
      try {
        WSResponse response = WS.url("http://localhost:" + PORT + "/proxy")
            .setQueryParameter("message", "Test Message through Client").get().get(TIMEOUT);
        assertThat(response.getStatus(), is(401));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testD2RequestWrongAuthorization() {
    running(testServer(PORT), () -> {
      try {
        WSResponse response;

        response = authenticateClient(TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
        String clientAccessToken = response.asJson().findPath("accessToken").textValue();

        response = WS.url("http://localhost:" + PORT + "/proxy")
            .setQueryParameter("message", "Test Message through Client")
            .setHeader("Authorization", "Bearer " + clientAccessToken).get().get(TIMEOUT);
        assertThat(response.getStatus(), is(401));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testDirectRequest() {
    running(testServer(PORT), () -> {
      try {
        WSResponse response;

        response = authenticateClient(TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
        String clientAccessToken = response.asJson().findPath("accessToken").textValue();

        response = authenticateUser(clientAccessToken, "test", USER_PASSWORD);
        String userAccessToken = response.asJson().findPath("accessToken").textValue();

        TransceiverWithAuthorization transceiver =
            new TransceiverWithAuthorization(new URL("http://localhost:" + PORT + "/example"),
                userAccessToken);
        Example example = getAvroComponent().client(Example.class, transceiver);
        assertThat(example.echo("Test Message"), is("Test Message"));

        ExampleClient exampleClient = getAvroComponent().client(ExampleClient.class, transceiver);
        assertThat(exampleClient.echo("Test Message").get(TIMEOUT), is("Test Message"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testDirectRequestMissingAuthorization() {
    running(testServer(PORT), () -> {
      try {
        URL url = new URL("http://localhost:" + PORT + "/example");
        Example example = getAvroComponent().client(Example.class, url);
        example.echo("Test Message");
        fail("AvroRuntimeException is expected");
      } catch (AvroRuntimeException e) {
        assertThat(e.getCause().getMessage(),
            is("Server returned HTTP response code 401 at URL http://localhost:" + PORT
                + "/example"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testDirectRequestWrongAuthorization() {
    running(testServer(PORT), () -> {
      try {
        WSResponse response = authenticateClient(TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
        String clientAccessToken = response.asJson().findPath("accessToken").textValue();

        TransceiverWithAuthorization transceiver =
            new TransceiverWithAuthorization(new URL("http://localhost:" + PORT + "/example"),
                clientAccessToken);
        Example example = getAvroComponent().client(Example.class, transceiver);
        example.echo("Test Message");
        fail("AvroRuntimeException is expected");
      } catch (AvroRuntimeException e) {
        assertThat(e.getCause().getMessage(),
            is("Server returned HTTP response code 401 at URL http://localhost:" + PORT
                + "/example"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testUserAccessDeniedWithUntrustedClient() {
    running(testServer(PORT), () -> {
      WSResponse response;

      response = authenticateClient(UNTRUSTED_CLIENT_ID, UNTRUSTED_CLIENT_SECRET);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = authenticateUser(clientAccessToken, "test", USER_PASSWORD);
      assertThat(response.getStatus(), is(401));
    });
  }

  private WSResponse authenticateClient(String clientId, String clientSecret) {
    ObjectNode request = Json.newObject();
    request.putAll(ImmutableMap
        .of("clientId", request.textNode(clientId), "clientSecret", request.textNode(clientSecret)));
    return WS.url("http://localhost:" + PORT + "/client/authenticate").post(request).get(TIMEOUT);
  }

  private WSResponse authenticateUser(String clientAccessToken, String username, String password) {
    ObjectNode request = Json.newObject();
    request.putAll(ImmutableMap.of("username", request.textNode(username), "password",
        request.textNode(password)));
    return WS.url("http://localhost:" + PORT + "/user/authenticate")
        .setHeader("Authorization", "Bearer " + clientAccessToken).post(request).get(TIMEOUT);
  }

  private AvroComponent getAvroComponent() {
    return ApplicationManager.getApplicationManager().getBean(AvroComponent.class);
  }

  private HttpRequestPoster getRequestPoster() {
    return ApplicationManager.getApplicationManager().getBean(HttpRequestPoster.class);
  }
}
