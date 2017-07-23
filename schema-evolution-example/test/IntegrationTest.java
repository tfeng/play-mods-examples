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

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.apache.avro.ipc.HttpTransceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import akka.stream.javadsl.StreamConverters;
import controllers.protocols.Date;
import controllers.protocols.Employee;
import controllers.protocols.EmployeeRegistry;
import controllers.protocols.Gender;
import me.tfeng.playmods.spring.ApplicationLoader;
import me.tfeng.playmods.spring.ExceptionWrapper;
import play.Application;
import play.ApplicationLoader.Context;
import play.Environment;
import play.libs.ws.SourceBodyWritable;
import play.libs.ws.StandaloneWSResponse;
import play.libs.ws.ahc.StandaloneAhcWSClient;
import play.libs.ws.ahc.StandaloneAhcWSRequest;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IntegrationTest {

  private static final int PORT = 9000;

  private static final Random RANDOM = new Random();

  private Application application;

  @Before
  public void setup() {
    application = new ApplicationLoader().load(new Context(Environment.simple()));
  }

  @Test
  public void testD2RequestWithCurrentProtocol() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response;

      response = post(client, "/current/countEmployees", "");
      assertThat(response.getStatus(), is(200));
      assertThat(Integer.parseInt(response.getBody()), is(0));

      response = post(client, "/current/addEmployee",
          "{\"employee\": {\"firstName\": \"Thomas\", \"lastName\": \"Feng\", \"gender\": \"MALE\", \"dateOfBirth\": {\"year\": 2000, \"month\": 1, \"day\": 1}}}");
      assertThat(response.getStatus(), is(200));
      assertThat(Long.parseLong(response.getBody()), is(1l));

      response = post(client, "/current/addEmployee",
          "{\"employee\": {\"firstName\": \"Jackson\", \"lastName\": \"Wang\", \"gender\": \"MALE\", \"dateOfBirth\": {\"year\": 2001, \"month\": 5, \"day\": 15}}}");
      assertThat(response.getStatus(), is(200));
      assertThat(Long.parseLong(response.getBody()), is(2l));

      response = post(client, "/current/addEmployee",
          "{\"employee\": {\"firstName\": \"Christine\", \"lastName\": \"Lee\", \"gender\": \"FEMALE\", \"dateOfBirth\": {\"year\": 2000, \"month\": 8, \"day\": 20}}}");
      assertThat(response.getStatus(), is(200));
      assertThat(Long.parseLong(response.getBody()), is(3l));

      response = post(client, "/current/countEmployees", "");
      assertThat(response.getStatus(), is(200));
      assertThat(Integer.parseInt(response.getBody()), is(3));

      response = post(client, "/current/makeManager", "{\"managerId\": 1, \"employeeId\": 2}");
      assertThat(response.getStatus(), is(200));

      response = post(client, "/current/makeManager", "{\"managerId\": 1, \"employeeId\": 3}");
      assertThat(response.getStatus(), is(200));

      response = post(client, "/current/getEmployees", "{\"managerId\": 1}");
      assertThat(response.getStatus(), is(200));
      assertThat(response.getBody(),
          is("[{\"id\":2,\"firstName\":\"Jackson\",\"lastName\":\"Wang\",\"gender\":\"MALE\",\"dateOfBirth\":{\"year\":2001,\"month\":5,\"day\":15}},{\"id\":3,\"firstName\":\"Christine\",\"lastName\":\"Lee\",\"gender\":\"FEMALE\",\"dateOfBirth\":{\"year\":2000,\"month\":8,\"day\":20}}]"));

      response = post(client, "/current/getManager", "{\"employeeId\": 2}");
      assertThat(response.getStatus(), is(200));
      assertThat(response.getBody(),
          is("{\"id\":1,\"firstName\":\"Thomas\",\"lastName\":\"Feng\",\"gender\":\"MALE\",\"dateOfBirth\":{\"year\":2000,\"month\":1,\"day\":1}}"));

      response = post(client, "/current/getManager", "{\"employeeId\": 3}");
      assertThat(response.getStatus(), is(200));
      assertThat(response.getBody(),
          is("{\"id\":1,\"firstName\":\"Thomas\",\"lastName\":\"Feng\",\"gender\":\"MALE\",\"dateOfBirth\":{\"year\":2000,\"month\":1,\"day\":1}}"));
    }));
  }

  @Test
  public void testD2RequestWithLegacyProtocol() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      StandaloneAhcWSClient client = application.injector().instanceOf(StandaloneAhcWSClient.class);
      StandaloneWSResponse response;

      response = get(client, "/legacy/countEmployees", null, null, null, null);
      assertThat(response.getStatus(), is(400));

      response = get(client, "/legacy/addEmployee", "firstName", "Thomas", "lastName", "Feng");
      assertThat(response.getStatus(), is(200));
      assertThat(Long.parseLong(response.getBody()), is(1l));

      response = get(client, "/legacy/addEmployee", "firstName", "Jackson", "lastName", "Wang");
      assertThat(response.getStatus(), is(200));
      assertThat(Long.parseLong(response.getBody()), is(2l));

      response = get(client, "/legacy/addEmployee", "firstName", "Christine", "lastName", "Lee");
      assertThat(response.getStatus(), is(200));
      assertThat(Long.parseLong(response.getBody()), is(3l));

      response = get(client, "/legacy/makeManager", "managerId", "1", "employeeId", "2");
      assertThat(response.getStatus(), is(200));

      response = get(client, "/legacy/makeManager", "managerId", "1", "employeeId", "3");
      assertThat(response.getStatus(), is(200));

      response = get(client, "/legacy/getEmployees", "managerId", "1", null, null);
      assertThat(response.getStatus(), is(200));
      assertThat(response.getBody(),
          is("[{\"id\": 2, \"firstName\": \"Jackson\", \"lastName\": \"Wang\"}, {\"id\": 3, \"firstName\": \"Christine\", \"lastName\": \"Lee\"}]"));

      response = get(client, "/legacy/getManager", "employeeId", "2", null, null);
      assertThat(response.getStatus(), is(200));
      assertThat(response.getBody(), is("{\"id\": 1, \"firstName\": \"Thomas\", \"lastName\": \"Feng\"}"));

      response = get(client, "/legacy/getManager", "employeeId", "3", null, null);
      assertThat(response.getStatus(), is(200));
      assertThat(response.getBody(), is("{\"id\": 1, \"firstName\": \"Thomas\", \"lastName\": \"Feng\"}"));
    }));
  }

  @Test
  public void testDirectRequest() {
    running(testServer(PORT, application), () -> {
      try {
        HttpTransceiver transceiver = new HttpTransceiver(new URL("http://localhost:" + PORT + "/employeeRegistry"));
        EmployeeRegistry registry = SpecificRequestor.getClient(EmployeeRegistry.class, transceiver);

        assertThat(registry.countEmployees(), is(0));

        Employee thomas = getEmployee("Thomas", "Feng", true);
        thomas.setId(registry.addEmployee(thomas));
        assertThat(thomas.getId(), is(1l));

        Employee jackson = getEmployee("Jackson", "Wang", true);
        jackson.setId(registry.addEmployee(jackson));
        assertThat(jackson.getId(), is(2l));

        Employee christine = getEmployee("Christine", "Lee", true);
        christine.setId(registry.addEmployee(christine));
        assertThat(christine.getId(), is(3l));
        assertThat(registry.countEmployees(), is(3));

        registry.makeManager(thomas.getId(), jackson.getId());
        registry.makeManager(thomas.getId(), christine.getId());

        assertThat(registry.getEmployees(thomas.getId()), is(ImmutableList.of(jackson, christine)));
        assertThat(registry.getManager(jackson.getId()), is(thomas));
        assertThat(registry.getManager(christine.getId()), is(thomas));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private StandaloneWSResponse get(StandaloneAhcWSClient client, String endpoint, String queryParameterName1,
      String queryParameterValue1, String queryParameterName2, String queryParameterValue2) throws ExecutionException,
      InterruptedException {
    StandaloneAhcWSRequest request = client.url("http://localhost:" + PORT + endpoint);
    if (queryParameterName1 != null) {
      request = request.addQueryParameter(queryParameterName1, queryParameterValue1);
    }
    if (queryParameterName2 != null) {
      request = request.addQueryParameter(queryParameterName2, queryParameterValue2);
    }
    return request.get().toCompletableFuture().get();
  }

  private Employee getEmployee(String firstName, String lastName, boolean generateNewFields) {
    Employee employee = new Employee();
    employee.setFirstName(firstName);
    employee.setLastName(lastName);
    if (generateNewFields) {
      employee.setGender(RANDOM.nextBoolean() ? Gender.MALE : Gender.FEMALE);
      employee.setDateOfBirth(getRandomDate());
    }
    return employee;
  }

  private Date getRandomDate() {
    Date date = new Date();
    date.setYear(1970 + RANDOM.nextInt() % 30);
    date.setMonth(RANDOM.nextInt() % 12);
    date.setDay(RANDOM.nextInt() % 28);
    return date;
  }

  private StandaloneWSResponse post(StandaloneAhcWSClient client, String endpoint, String data)
      throws ExecutionException, InterruptedException {
    return post(client, endpoint, null, null, data);
  }

  private StandaloneWSResponse post(StandaloneAhcWSClient client, String endpoint, String headerName,
      String headerValue, String data) throws ExecutionException, InterruptedException {
    StandaloneAhcWSRequest request = client.url("http://localhost:" + PORT + endpoint).setContentType("avro/json");
    if (headerName != null) {
      request = request.addHeader(headerName, headerValue);
    }
    SourceBodyWritable bodyWritable = new SourceBodyWritable(
        StreamConverters.fromInputStream(() -> new ByteArrayInputStream(data.getBytes())));
    return request.post(bodyWritable).toCompletableFuture().get();
  }
}
