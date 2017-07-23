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
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import akka.stream.javadsl.StreamConverters;
import me.tfeng.playmods.oauth2.ApplicationLoader;
import me.tfeng.playmods.spring.ExceptionWrapper;
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

  private static final int PORT = 3333;

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
  public void testClientAuthenticationFailure() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response;

      response = authenticateClient(client, TRUSTED_CLIENT_ID, "wrong_" + TRUSTED_CLIENT_SECRET);
      assertThat(response.getStatus(), is(401));

      response = authenticateClient(client, "wrong_" + TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      assertThat(response.getStatus(), is(401));
    }));
  }

  @Test
  public void testClientAuthenticationSuccess() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response = authenticateClient(client, TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      JsonNode json = Json.parse(response.getBody());
      assertThat(json.findPath("accessToken").textValue(), not(isEmptyString()));
      assertThat(json.findPath("clientId").textValue(), is(TRUSTED_CLIENT_ID));
      assertThat(json.findPath("expiration").longValue(), greaterThan(System.currentTimeMillis()));
    }));
  }

  @Test
  public void testUserAccessDenied() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response;

      response = authenticateClient(client, TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = Json.parse(response.getBody()).findPath("accessToken").textValue();

      response = authenticateUser(client, clientAccessToken, "test", "wrong_" + USER_PASSWORD);
      assertThat(response.getStatus(), is(401));
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

  @Test
  public void testUserAccessTokenRefreshSuccess() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response;
      JsonNode json;

      response = authenticateClient(client, TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = Json.parse(response.getBody()).findPath("accessToken").textValue();

      response = authenticateUser(client, clientAccessToken, "test", USER_PASSWORD);
      json = Json.parse(response.getBody());
      assertThat(json.findPath("accessToken").textValue(), not(isEmptyString()));
      assertThat(json.findPath("refreshToken").textValue(), not(isEmptyString()));
      assertThat(json.findPath("username").textValue(), is("test"));
      assertThat(json.findPath("expiration").longValue(), greaterThan(System.currentTimeMillis()));
      String oldUserAccessToken = json.findPath("accessToken").textValue();
      String refreshToken = json.findPath("refreshToken").textValue();

      response = getUser(client, oldUserAccessToken);
      json = Json.parse(response.getBody());
      assertThat(json.findPath("username").textValue(), is("test"));
      assertThat(json.findPath("isActive").booleanValue(), is(true));

      response = refreshUserAccessToken(client, clientAccessToken, refreshToken);
      json = Json.parse(response.getBody());
      assertThat(json.findPath("accessToken").textValue(), not(isEmptyString()));
      assertThat(json.findPath("refreshToken").textValue(), is(refreshToken));
      assertThat(json.findPath("expiration").longValue(), greaterThan(System.currentTimeMillis()));
      String newUserAccessToken = json.findPath("accessToken").textValue();

      response = getUser(client, newUserAccessToken);
      json = Json.parse(response.getBody());
      assertThat(json.findPath("username").textValue(), is("test"));
      assertThat(json.findPath("isActive").booleanValue(), is(true));

      response = getUser(client, oldUserAccessToken);
      assertThat(response.getStatus(), is(401));
    }));
  }

  @Test
  public void testUserAuthenticationFailure() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response = getUser(client, "1234-abcd");
      assertThat(response.getStatus(), is(401));
    }));
  }

  @Test
  public void testUserAuthenticationSuccess() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response;
      JsonNode json;

      response = authenticateClient(client, TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = Json.parse(response.getBody()).findPath("accessToken").textValue();

      response = authenticateUser(client, clientAccessToken, "test", USER_PASSWORD);
      json = Json.parse(response.getBody());
      assertThat(json.findPath("accessToken").textValue(), not(isEmptyString()));
      assertThat(json.findPath("refreshToken").textValue(), not(isEmptyString()));
      assertThat(json.findPath("username").textValue(), is("test"));
      assertThat(json.findPath("expiration").longValue(), greaterThan(System.currentTimeMillis()));
      String userAccessToken = json.findPath("accessToken").textValue();

      response = getUser(client, userAccessToken);
      json = Json.parse(response.getBody());
      assertThat(json.findPath("username").textValue(), is("test"));
      assertThat(json.findPath("isActive").booleanValue(), is(true));
    }));
  }

  @Test
  public void testUserNotAuthenticated() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response;

      response = getUser(client, null);
      assertThat(response.getStatus(), is(401));

      response = authenticateClient(client, TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = Json.parse(response.getBody()).findPath("accessToken").textValue();

      response = getUser(client, clientAccessToken);
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
    if (queryParameterName != null) {
      request = request.addQueryParameter(queryParameterName, queryParameterValue);
    }
    return request.get().toCompletableFuture().get();
  }

  private StandaloneWSResponse getUser(StandaloneAhcWSClient client, String userAccessToken) throws ExecutionException,
      InterruptedException {
    return get(client, "/user/get", "Authorization", "Bearer " + userAccessToken, null, null);
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

  private StandaloneWSResponse refreshUserAccessToken(StandaloneAhcWSClient client, String clientAccessToken,
      String refreshToken) throws ExecutionException, InterruptedException {
    ObjectNode body = Json.newObject();
    body.setAll(ImmutableMap.of("refreshToken", body.textNode(refreshToken)));
    return post(client, "/user/refresh", "Authorization", "Bearer " + clientAccessToken, body.toString());
  }
}
