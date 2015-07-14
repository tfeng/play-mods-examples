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

package controllers;

import java.io.IOException;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import me.tfeng.playmods.avro.AvroHelper;
import me.tfeng.playmods.avro.d2.AvroD2Client;
import me.tfeng.playmods.avro.d2.factories.ClientFactory;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component
public class LegacyProtocolController {

  @Autowired
  private ClientFactory clientFactory;

  private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

  private static final Protocol LEGACY_PROTOCOL;

  private static final ALogger LOG = Logger.of(LegacyProtocolController.class);

  static {
    try {
      LEGACY_PROTOCOL = Protocol.parse(LegacyProtocolController.class.getResourceAsStream(
          "/legacy/employee_registry.avpr"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Promise<Result> addEmployee(String firstName, String lastName) throws Exception {
    Schema requestSchema = LEGACY_PROTOCOL.getMessages().get("addEmployee").getRequest();

    Schema employeeSchema = requestSchema.getField("employee").schema();
    ObjectNode employeeValue = new ObjectNode(JSON_NODE_FACTORY);
    employeeValue.put("firstName", firstName);
    employeeValue.put("lastName", lastName);

    JsonNode record = AvroHelper.convertFromSimpleRecord(employeeSchema, employeeValue);
    Object[] args =
        new Object[] {AvroHelper.createGenericRequestFromRecord(employeeSchema, record)};
    return invoke("addEmployee", args);
  }

  public Promise<Result> countEmployees() throws Exception {
    Object[] args = new Object[0];
    return invoke("countEmployees", args);
  }

  public Promise<Result> getEmployees(long managerId) throws Exception {
    Object[] args = new Object[] {managerId};
    return invoke("getEmployees", args);
  }

  public Promise<Result> getManager(long employeeId) throws Exception {
    Object[] args = new Object[] {employeeId};
    return invoke("getManager", args);
  }

  public Promise<Result> makeManager(long managerId, long employeeId) throws Exception {
    Object[] args = new Object[] {managerId, employeeId};
    return invoke("makeManager", args);
  }

  public Promise<Result> removeEmployee(long employeeId) throws Exception {
    Object[] args = new Object[] {employeeId};
    return invoke("removeEmployee", args);
  }

  private Promise<Result> invoke(String method, Object[] args) throws Exception {
    AvroD2Client client = clientFactory.create(LEGACY_PROTOCOL, new SpecificData(), true);
    return client.request(method, args)
        .<Result>map(result -> Results.ok(String.valueOf(result)))
        .recover(e -> {
          try {
            LOG.warn("Exception thrown while processing request; returning bad request", e);
            return Results.badRequest(e.getLocalizedMessage());
          } catch (Exception e2) {
            throw e;
          }
        });
  }
}
