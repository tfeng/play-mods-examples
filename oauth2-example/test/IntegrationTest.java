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

import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import me.tfeng.playmods.oauth2.ApplicationLoader;
import me.tfeng.playmods.spring.ExceptionWrapper;
import play.Application;
import play.ApplicationLoader.Context;
import play.Environment;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

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
    running(testServer(PORT, application), () -> {
      WSResponse response;

      response = authenticateClient(TRUSTED_CLIENT_ID, "wrong_" + TRUSTED_CLIENT_SECRET);
      assertThat(response.getStatus(), is(401));

      response = authenticateClient("wrong_" + TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      assertThat(response.getStatus(), is(401));
    });
  }

  @Test
  public void testClientAuthenticationSuccess() {
    running(testServer(PORT, application), () -> {
      WSResponse response = authenticateClient(TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      JsonNode json = response.asJson();
      assertThat(json.findPath("accessToken").textValue(), not(isEmptyString()));
      assertThat(json.findPath("clientId").textValue(), is(TRUSTED_CLIENT_ID));
      assertThat(json.findPath("expiration").longValue(), greaterThan(System.currentTimeMillis()));
    });
  }

  @Test
  public void testUserAccessDenied() {
    running(testServer(PORT, application), () -> {
      WSResponse response;

      response = authenticateClient(TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = authenticateUser(clientAccessToken, "test", "wrong_" + USER_PASSWORD);
      assertThat(response.getStatus(), is(401));
    });
  }

  @Test
  public void testUserAccessDeniedWithUntrustedClient() {
    running(testServer(PORT, application), () -> {
      WSResponse response;

      response = authenticateClient(UNTRUSTED_CLIENT_ID, UNTRUSTED_CLIENT_SECRET);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = authenticateUser(clientAccessToken, "test", USER_PASSWORD);
      assertThat(response.getStatus(), is(401));
    });
  }

  @Test
  public void testUserAccessTokenRefreshSuccess() {
    running(testServer(PORT, application), () -> {
      WSResponse response;
      JsonNode json;

      response = authenticateClient(TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = authenticateUser(clientAccessToken, "test", USER_PASSWORD);
      json = response.asJson();
      assertThat(json.findPath("accessToken").textValue(), not(isEmptyString()));
      assertThat(json.findPath("refreshToken").textValue(), not(isEmptyString()));
      assertThat(json.findPath("username").textValue(), is("test"));
      assertThat(json.findPath("expiration").longValue(), greaterThan(System.currentTimeMillis()));
      String oldUserAccessToken = json.findPath("accessToken").textValue();
      String refreshToken = json.findPath("refreshToken").textValue();

      response = getUser(oldUserAccessToken);
      json = response.asJson();
      assertThat(json.findPath("username").textValue(), is("test"));
      assertThat(json.findPath("isActive").booleanValue(), is(true));

      response = refreshUserAccessToken(clientAccessToken, refreshToken);
      json = response.asJson();
      assertThat(json.findPath("accessToken").textValue(), not(isEmptyString()));
      assertThat(json.findPath("refreshToken").textValue(), is(refreshToken));
      assertThat(json.findPath("expiration").longValue(), greaterThan(System.currentTimeMillis()));
      String newUserAccessToken = json.findPath("accessToken").textValue();

      response = getUser(newUserAccessToken);
      json = response.asJson();
      assertThat(json.findPath("username").textValue(), is("test"));
      assertThat(json.findPath("isActive").booleanValue(), is(true));

      response = getUser(oldUserAccessToken);
      assertThat(response.getStatus(), is(401));
    });
  }

  @Test
  public void testUserAuthenticationFailure() {
    running(testServer(PORT, application), () -> {
      WSResponse response = getUser("1234-abcd");
      assertThat(response.getStatus(), is(401));
    });
  }

  @Test
  public void testUserAuthenticationSuccess() {
    running(testServer(PORT, application), () -> {
      WSResponse response;
      JsonNode json;

      response = authenticateClient(TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = authenticateUser(clientAccessToken, "test", USER_PASSWORD);
      json = response.asJson();
      assertThat(json.findPath("accessToken").textValue(), not(isEmptyString()));
      assertThat(json.findPath("refreshToken").textValue(), not(isEmptyString()));
      assertThat(json.findPath("username").textValue(), is("test"));
      assertThat(json.findPath("expiration").longValue(), greaterThan(System.currentTimeMillis()));
      String userAccessToken = json.findPath("accessToken").textValue();

      response = getUser(userAccessToken);
      json = response.asJson();
      assertThat(json.findPath("username").textValue(), is("test"));
      assertThat(json.findPath("isActive").booleanValue(), is(true));
    });
  }

  @Test
  public void testUserNotAuthenticated() {
    running(testServer(PORT, application), () -> {
      WSResponse response;

      response = getUser(null);
      assertThat(response.getStatus(), is(401));

      response = authenticateClient(TRUSTED_CLIENT_ID, TRUSTED_CLIENT_SECRET);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = getUser(clientAccessToken);
      assertThat(response.getStatus(), is(401));
    });
  }

  private WSResponse authenticateClient(String clientId, String clientSecret) {
    ObjectNode body = Json.newObject();
    body.setAll(ImmutableMap.of("clientId", body.textNode(clientId), "clientSecret", body.textNode(clientSecret)));
    WSClient client = WS.newClient(PORT);
    WSRequest request = client.url("/client/authenticate");
    return ExceptionWrapper.wrap(() -> request.post(body).toCompletableFuture().get());
  }

  private WSResponse authenticateUser(String clientAccessToken, String username, String password) {
    ObjectNode body = Json.newObject();
    body.setAll(ImmutableMap.of("username", body.textNode(username), "password", body.textNode(password)));
    WSClient client = WS.newClient(PORT);
    WSRequest request = client.url("/user/authenticate").setHeader("Authorization", "Bearer " + clientAccessToken);
    return ExceptionWrapper.wrap(() -> request.post(body).toCompletableFuture().get());
  }

  private WSResponse getUser(String userAccessToken) {
    WSClient client = WS.newClient(PORT);
    WSRequest request = client.url("http://localhost:3333/user/get");
    if (userAccessToken != null) {
      request.setHeader("Authorization", "Bearer " + userAccessToken);
    }
    return ExceptionWrapper.wrap(() -> request.get().toCompletableFuture().get());
  }

  private WSResponse refreshUserAccessToken(String clientAccessToken, String refreshToken) {
    ObjectNode body = Json.newObject();
    body.setAll(ImmutableMap.of("refreshToken", body.textNode(refreshToken)));
    WSClient client = WS.newClient(PORT);
    WSRequest request = client.url("/user/refresh").setHeader("Authorization", "Bearer " + clientAccessToken);
    return ExceptionWrapper.wrap(() -> request.post(body).toCompletableFuture().get());
  }
}
