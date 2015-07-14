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
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IntegrationTest {

  private static final int TIMEOUT = Integer.MAX_VALUE;

  private String trustedClientId = "trusted-client";
  private String trustedClientSecret = "trusted-client-password";
  private String untrustedClientId = "untrusted-client";
  private String untrustedClientSecret = "untrusted-client-password";
  private String userPassword = "password";

  @Test
  public void testClientAuthenticationFailure() {
    running(testServer(3333), () -> {
      WSResponse response;

      response = authenticateClient(trustedClientId, "wrong_" + trustedClientSecret);
      assertThat(response.getStatus(), is(401));

      response = authenticateClient("wrong_" + trustedClientId, trustedClientSecret);
      assertThat(response.getStatus(), is(401));
    });
  }

  @Test
  public void testClientAuthenticationSuccess() {
    running(testServer(3333), () -> {
      WSResponse response = authenticateClient(trustedClientId, trustedClientSecret);
      JsonNode json = response.asJson();
      assertThat(json.findPath("accessToken").textValue(), not(isEmptyString()));
      assertThat(json.findPath("clientId").textValue(), is(trustedClientId));
      assertThat(json.findPath("expiration").longValue(), greaterThan(System.currentTimeMillis()));
    });
  }

  @Test
  public void testUserAccessDenied() {
    running(testServer(3333), () -> {
      WSResponse response;

      response = authenticateClient(trustedClientId, trustedClientSecret);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = authenticateUser(clientAccessToken, "test", "wrong_" + userPassword);
      assertThat(response.getStatus(), is(401));
    });
  }

  @Test
  public void testUserAccessDeniedWithUntrustedClient() {
    running(testServer(3333), () -> {
      WSResponse response;

      response = authenticateClient(untrustedClientId, untrustedClientSecret);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = authenticateUser(clientAccessToken, "test", userPassword);
      assertThat(response.getStatus(), is(401));
    });
  }

  @Test
  public void testUserAccessTokenRefreshSuccess() {
    running(testServer(3333), () -> {
      WSResponse response;
      JsonNode json;

      response = authenticateClient(trustedClientId, trustedClientSecret);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = authenticateUser(clientAccessToken, "test", userPassword);
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
    running(testServer(3333), () -> {
      WSResponse response = getUser("1234-abcd");
      assertThat(response.getStatus(), is(401));
    });
  }

  @Test
  public void testUserAuthenticationSuccess() {
    running(testServer(3333), () -> {
      WSResponse response;
      JsonNode json;

      response = authenticateClient(trustedClientId, trustedClientSecret);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = authenticateUser(clientAccessToken, "test", userPassword);
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
    running(testServer(3333), () -> {
      WSResponse response;

      response = getUser(null);
      assertThat(response.getStatus(), is(401));

      response = authenticateClient(trustedClientId, trustedClientSecret);
      String clientAccessToken = response.asJson().findPath("accessToken").textValue();

      response = getUser(clientAccessToken);
      assertThat(response.getStatus(), is(401));
    });
  }

  private WSResponse authenticateClient(String clientId, String clientSecret) {
    ObjectNode request = Json.newObject();
    request.setAll(ImmutableMap
        .of("clientId", request.textNode(clientId), "clientSecret", request.textNode(clientSecret)));
    return WS.url("http://localhost:3333/client/authenticate").post(request).get(TIMEOUT);
  }

  private WSResponse authenticateUser(String clientAccessToken, String username, String password) {
    ObjectNode request = Json.newObject();
    request.setAll(ImmutableMap
        .of("username", request.textNode(username), "password", request.textNode(password)));
    return WS.url("http://localhost:3333/user/authenticate")
        .setHeader("Authorization", "Bearer " + clientAccessToken).post(request).get(TIMEOUT);
  }

  private WSResponse getUser(String userAccessToken) {
    WSRequest requestHolder = WS.url("http://localhost:3333/user/get");
    if (userAccessToken != null) {
      requestHolder.setHeader("Authorization", "Bearer " + userAccessToken);
    }
    return requestHolder.get().get(TIMEOUT);
  }

  private WSResponse refreshUserAccessToken(String clientAccessToken, String refreshToken) {
    ObjectNode request = Json.newObject();
    request.setAll(ImmutableMap.of("refreshToken", request.textNode(refreshToken)));
    return WS.url("http://localhost:3333/user/refresh")
        .setHeader("Authorization", "Bearer " + clientAccessToken).post(request).get(TIMEOUT);
  }
}
