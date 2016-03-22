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

import java.net.URL;
import java.util.Random;

import org.apache.avro.ipc.HttpTransceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import controllers.protocols.Date;
import controllers.protocols.Employee;
import controllers.protocols.EmployeeRegistry;
import controllers.protocols.Gender;
import me.tfeng.playmods.spring.ApplicationLoader;
import me.tfeng.playmods.spring.ExceptionWrapper;
import play.Application;
import play.ApplicationLoader.Context;
import play.Environment;
import play.libs.ws.WS;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;

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
      WSResponse response;
      WSClient client = WS.newClient(PORT);

      response = client.url("/current/countEmployees")
          .setHeader("Content-Type", "avro/json")
          .post("")
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));
      assertThat(Integer.parseInt(response.getBody()), is(0));

      response = client.url("/current/addEmployee")
          .setHeader("Content-Type", "avro/json")
          .post("{\"employee\": {\"firstName\": \"Thomas\", \"lastName\": \"Feng\", \"gender\": \"MALE\", \"dateOfBirth\": {\"year\": 2000, \"month\": 1, \"day\": 1}}}")
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));
      assertThat(Long.parseLong(response.getBody()), is(1l));

      response = client.url("/current/addEmployee")
          .setHeader("Content-Type", "avro/json")
          .post("{\"employee\": {\"firstName\": \"Jackson\", \"lastName\": \"Wang\", \"gender\": \"MALE\", \"dateOfBirth\": {\"year\": 2001, \"month\": 5, \"day\": 15}}}")
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));
      assertThat(Long.parseLong(response.getBody()), is(2l));

      response = client.url("/current/addEmployee")
          .setHeader("Content-Type", "avro/json")
          .post("{\"employee\": {\"firstName\": \"Christine\", \"lastName\": \"Lee\", \"gender\": \"FEMALE\", \"dateOfBirth\": {\"year\": 2000, \"month\": 8, \"day\": 20}}}")
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));
      assertThat(Long.parseLong(response.getBody()), is(3l));

      response = client.url("/current/countEmployees")
          .setHeader("Content-Type", "avro/json")
          .post("")
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));
      assertThat(Integer.parseInt(response.getBody()), is(3));

      response = client.url("/current/makeManager")
          .setHeader("Content-Type", "avro/json")
          .post("{\"managerId\": 1, \"employeeId\": 2}")
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));

      response = client.url("/current/makeManager")
          .setHeader("Content-Type", "avro/json")
          .post("{\"managerId\": 1, \"employeeId\": 3}")
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));

      response = client.url("/current/getEmployees")
          .setHeader("Content-Type", "avro/json")
          .post("{\"managerId\": 1}")
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));
      assertThat(response.getBody(),
          is("[{\"id\":2,\"firstName\":\"Jackson\",\"lastName\":\"Wang\",\"gender\":\"MALE\",\"dateOfBirth\":{\"year\":2001,\"month\":5,\"day\":15}},{\"id\":3,\"firstName\":\"Christine\",\"lastName\":\"Lee\",\"gender\":\"FEMALE\",\"dateOfBirth\":{\"year\":2000,\"month\":8,\"day\":20}}]"));

      response = client.url("/current/getManager")
          .setHeader("Content-Type", "avro/json")
          .post("{\"employeeId\": 2}")
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));
      assertThat(response.getBody(),
          is("{\"id\":1,\"firstName\":\"Thomas\",\"lastName\":\"Feng\",\"gender\":\"MALE\",\"dateOfBirth\":{\"year\":2000,\"month\":1,\"day\":1}}"));

      response = client.url("/current/getManager")
          .setHeader("Content-Type", "avro/json")
          .post("{\"employeeId\": 3}")
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));
      assertThat(response.getBody(),
          is("{\"id\":1,\"firstName\":\"Thomas\",\"lastName\":\"Feng\",\"gender\":\"MALE\",\"dateOfBirth\":{\"year\":2000,\"month\":1,\"day\":1}}"));
    }));
  }

  @Test
  public void testD2RequestWithLegacyProtocol() {
    running(testServer(PORT, application), ExceptionWrapper.wrapFunction(() -> {
      WSResponse response;
      WSClient client = WS.newClient(PORT);

      response = client.url("/legacy/countEmployees").get().toCompletableFuture().get();
      assertThat(response.getStatus(), is(400));

      response = client.url("/legacy/addEmployee")
          .setQueryParameter("firstName", "Thomas")
          .setQueryParameter("lastName", "Feng")
          .get()
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));
      assertThat(Long.parseLong(response.getBody()), is(1l));

      response = client.url("/legacy/addEmployee")
          .setQueryParameter("firstName", "Jackson")
          .setQueryParameter("lastName", "Wang")
          .get()
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));
      assertThat(Long.parseLong(response.getBody()), is(2l));

      response = client.url("/legacy/addEmployee")
          .setQueryParameter("firstName", "Christine")
          .setQueryParameter("lastName", "Lee")
          .get()
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));
      assertThat(Long.parseLong(response.getBody()), is(3l));

      response = client.url("/legacy/makeManager")
          .setQueryParameter("managerId", "1")
          .setQueryParameter("employeeId", "2")
          .get()
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));

      response = client.url("/legacy/makeManager")
          .setQueryParameter("managerId", "1")
          .setQueryParameter("employeeId", "3")
          .get()
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));

      response = client.url("/legacy/getEmployees")
          .setQueryParameter("managerId", "1")
          .get()
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));
      assertThat(response.getBody(),
          is("[{\"id\": 2, \"firstName\": \"Jackson\", \"lastName\": \"Wang\"}, {\"id\": 3, \"firstName\": \"Christine\", \"lastName\": \"Lee\"}]"));

      response = client.url("/legacy/getManager")
          .setQueryParameter("employeeId", "2")
          .get()
          .toCompletableFuture()
          .get();
      assertThat(response.getStatus(), is(200));
      assertThat(response.getBody(), is("{\"id\": 1, \"firstName\": \"Thomas\", \"lastName\": \"Feng\"}"));

      response = client.url("/legacy/getManager")
          .setQueryParameter("employeeId", "3")
          .get()
          .toCompletableFuture()
          .get();
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
}
