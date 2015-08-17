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
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

import java.net.URL;
import java.util.Random;

import org.apache.avro.ipc.HttpTransceiver;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import controllers.protocols.Date;
import controllers.protocols.Employee;
import controllers.protocols.EmployeeRegistry;
import controllers.protocols.Gender;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IntegrationTest {

  private static final int PORT = 9000;

  private static final Random RANDOM = new Random();

  private static final int TIMEOUT = Integer.MAX_VALUE;

  @Test
  public void testD2RequestWithCurrentProtocol() {
    running(testServer(PORT), () -> {
      try {
        WSResponse response = WS.url("http://localhost:" + PORT + "/current/countEmployees")
            .setHeader("Content-Type", "avro/json")
            .post("")
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(Integer.parseInt(response.getBody()), is(0));

        response = WS.url("http://localhost:" + PORT + "/current/addEmployee")
            .setHeader("Content-Type", "avro/json")
            .post("{\"employee\": {\"firstName\": \"Thomas\", \"lastName\": \"Feng\", \"gender\": \"MALE\", \"dateOfBirth\": {\"year\": 2000, \"month\": 1, \"day\": 1}}}")
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(Long.parseLong(response.getBody()), is(1l));

        response = WS.url("http://localhost:" + PORT + "/current/addEmployee")
            .setHeader("Content-Type", "avro/json")
            .post("{\"employee\": {\"firstName\": \"Jackson\", \"lastName\": \"Wang\", \"gender\": \"MALE\", \"dateOfBirth\": {\"year\": 2001, \"month\": 5, \"day\": 15}}}")
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(Long.parseLong(response.getBody()), is(2l));

        response = WS.url("http://localhost:" + PORT + "/current/addEmployee")
            .setHeader("Content-Type", "avro/json")
            .post("{\"employee\": {\"firstName\": \"Christine\", \"lastName\": \"Lee\", \"gender\": \"FEMALE\", \"dateOfBirth\": {\"year\": 2000, \"month\": 8, \"day\": 20}}}")
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(Long.parseLong(response.getBody()), is(3l));

        response = WS.url("http://localhost:" + PORT + "/current/countEmployees")
            .setHeader("Content-Type", "avro/json")
            .post("")
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(Integer.parseInt(response.getBody()), is(3));

        response = WS.url("http://localhost:" + PORT + "/current/makeManager")
            .setHeader("Content-Type", "avro/json")
            .post("{\"managerId\": 1, \"employeeId\": 2}")
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));

        response = WS.url("http://localhost:" + PORT + "/current/makeManager")
            .setHeader("Content-Type", "avro/json")
            .post("{\"managerId\": 1, \"employeeId\": 3}")
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));

        response = WS.url("http://localhost:" + PORT + "/current/getEmployees")
            .setHeader("Content-Type", "avro/json")
            .post("{\"managerId\": 1}")
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(),
            is("[{\"id\":2,\"firstName\":\"Jackson\",\"lastName\":\"Wang\",\"gender\":\"MALE\",\"dateOfBirth\":{\"year\":2001,\"month\":5,\"day\":15}},{\"id\":3,\"firstName\":\"Christine\",\"lastName\":\"Lee\",\"gender\":\"FEMALE\",\"dateOfBirth\":{\"year\":2000,\"month\":8,\"day\":20}}]"));

        response = WS.url("http://localhost:" + PORT + "/current/getManager")
            .setHeader("Content-Type", "avro/json")
            .post("{\"employeeId\": 2}").get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(),
            is("{\"id\":1,\"firstName\":\"Thomas\",\"lastName\":\"Feng\",\"gender\":\"MALE\",\"dateOfBirth\":{\"year\":2000,\"month\":1,\"day\":1}}"));

        response = WS.url("http://localhost:" + PORT + "/current/getManager")
            .setHeader("Content-Type", "avro/json")
            .post("{\"employeeId\": 3}")
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(),
            is("{\"id\":1,\"firstName\":\"Thomas\",\"lastName\":\"Feng\",\"gender\":\"MALE\",\"dateOfBirth\":{\"year\":2000,\"month\":1,\"day\":1}}"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testD2RequestWithLegacyProtocol() {
    running(testServer(PORT), () -> {
      try {
        WSResponse response = WS.url("http://localhost:" + PORT + "/legacy/countEmployees").get().get(TIMEOUT);
        assertThat(response.getStatus(), is(400));

        response = WS.url("http://localhost:" + PORT + "/legacy/addEmployee")
            .setQueryParameter("firstName", "Thomas")
            .setQueryParameter("lastName", "Feng")
            .get()
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(Long.parseLong(response.getBody()), is(1l));

        response = WS.url("http://localhost:" + PORT + "/legacy/addEmployee")
            .setQueryParameter("firstName", "Jackson")
            .setQueryParameter("lastName", "Wang")
            .get()
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(Long.parseLong(response.getBody()), is(2l));

        response = WS.url("http://localhost:" + PORT + "/legacy/addEmployee")
            .setQueryParameter("firstName", "Christine")
            .setQueryParameter("lastName", "Lee")
            .get()
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(Long.parseLong(response.getBody()), is(3l));

        response = WS.url("http://localhost:" + PORT + "/legacy/makeManager")
            .setQueryParameter("managerId", "1")
            .setQueryParameter("employeeId", "2")
            .get()
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));

        response = WS.url("http://localhost:" + PORT + "/legacy/makeManager")
            .setQueryParameter("managerId", "1")
            .setQueryParameter("employeeId", "3")
            .get()
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));

        response = WS.url("http://localhost:" + PORT + "/legacy/getEmployees")
            .setQueryParameter("managerId", "1")
            .get()
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(),
            is("[{\"id\": 2, \"firstName\": \"Jackson\", \"lastName\": \"Wang\"}, {\"id\": 3, \"firstName\": \"Christine\", \"lastName\": \"Lee\"}]"));

        response = WS.url("http://localhost:" + PORT + "/legacy/getManager")
            .setQueryParameter("employeeId", "2")
            .get()
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("{\"id\": 1, \"firstName\": \"Thomas\", \"lastName\": \"Feng\"}"));

        response = WS.url("http://localhost:" + PORT + "/legacy/getManager")
            .setQueryParameter("employeeId", "3")
            .get()
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("{\"id\": 1, \"firstName\": \"Thomas\", \"lastName\": \"Feng\"}"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testDirectRequest() {
    running(testServer(PORT), () -> {
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
