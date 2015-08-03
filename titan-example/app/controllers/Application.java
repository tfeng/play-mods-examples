/**
 * Copyright 2015 Thomas Feng
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package controllers;

import org.apache.tinkerpop.gremlin.process.traversal.Compare;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.core.TitanVertex;

import beans.TitanServer;
import play.Logger;
import play.Logger.ALogger;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Service
public class Application extends Controller {

  private static final ALogger LOG = Logger.of(Application.class);

  @Autowired
  private TitanServer titan;

  public Promise<Result> addPerson(String name, int age) {
    TitanTransaction transaction = titan.getGraph().newTransaction();
    try {
      TitanVertex vertex = transaction.addVertex("Person");
      vertex.property("name", name);
      vertex.property("age", age);
      transaction.commit();
      return Promise.pure(Results.ok());
    } catch (Throwable t) {
      LOG.error("Unable to add person with name " + name + " and age " + age, t);
      throw t;
    }
  }

  public Promise<Result> getPerson(String name) {
    try {
      Iterable<TitanVertex> vertices = titan.getGraph().query().has("name", name).vertices();
      TitanVertex vertex = vertices.iterator().next();
      int age = vertex.value("age");
      return Promise.pure(Results.ok(name + " is " + age + " year(s) old.\n"));
    } catch (Throwable t) {
      LOG.error("Unable to get person with name " + name, t);
      throw t;
    }
  }

  public Promise<Result> getPersonsBetweenAges(int min, int max) {
    try {
      GraphTraversalSource traversal = titan.getGraph().traversal();
      GraphTraversal<Vertex, String> names = traversal.V()
          .has("age", new P(Compare.gte, min))
          .has("age", new P(Compare.lte, max))
          .values("name");
      if (names.hasNext()) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("The following person(s) are between " + min + " and " + max + ": ");
        names.forEachRemaining(name -> buffer.append(name + ", "));
        return Promise.pure(Results.ok(buffer.substring(0, buffer.length() - 2) + ".\n"));
      } else {
        return Promise.pure(Results.ok("No one is between " + min + " and " + max + ".\n"));
      }
    } catch (Throwable t) {
      LOG.error("Unable to get persons with ages between " + min + " and " + max, t);
      throw t;
    }
  }
}
