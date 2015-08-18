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

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.__;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.bothE;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.local;

import org.apache.tinkerpop.gremlin.process.traversal.Compare;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
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

  public Result addPerson(String name, int age) {
    TitanTransaction transaction = titan.getGraph().newTransaction();
    try {
      TitanVertex vertex = transaction.addVertex("Person");
      vertex.property("name", name);
      vertex.property("age", age);
      transaction.commit();
      return Results.ok();
    } finally {
      transaction.close();
    }
  }

  public Result getFriends(String name) {
    TitanTransaction transaction = titan.getGraph().newTransaction();
    try {
      GraphTraversalSource traversal = transaction.traversal();
      GraphTraversal<Vertex, Object> names = traversal.V()
          .has("name", name)
          .local(bothE("friend").order().by("strength", Order.decr))
          .otherV()
          .dedup()
          .values("name");
      if (names.hasNext()) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("The following person(s) are friends of " + name + ": ");
        names.forEachRemaining(friendName -> buffer.append(friendName + ", "));
        return Results.ok(buffer.substring(0, buffer.length() - 2) + ".\n");
      } else {
        return Results.ok("No one is friend of " + name + ".\n");
      }
    } catch (Exception e) {
      LOG.error("Unable to find friends", e);
      return Results.badRequest();
    } finally {
      transaction.close();
    }
  }

  public Result getMoreFriends(String name) {
    TitanTransaction transaction = titan.getGraph().newTransaction();
    try {
      GraphTraversalSource traversal = transaction.traversal();
      GraphTraversal<Vertex, Object> names = traversal.V()
          .has("name", name)
          .union(
              local(bothE("friend").order().by("strength", Order.decr)).otherV()
                  .union(__(), local(bothE("friend").order().by("strength", Order.decr)).otherV()),
              local(bothE("enemy").order().by("strength", Order.decr)).otherV()
                  .local(bothE("enemy").order().by("strength", Order.decr)).otherV())
          .simplePath()
          .dedup()
          .values("name");
      if (names.hasNext()) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("The following person(s) are more friends of " + name + ": ");
        names.forEachRemaining(friendName -> buffer.append(friendName + ", "));
        return Results.ok(buffer.substring(0, buffer.length() - 2) + ".\n");
      }
      return Results.ok("No one is friend of " + name + ".\n");
    } catch (Exception e) {
      LOG.error("Unable to find more friends", e);
      return Results.badRequest();
    } finally {
      transaction.close();
    }
  }

  public Result getPerson(String name) {
    TitanTransaction transaction = titan.getGraph().newTransaction();
    try {
      Iterable<TitanVertex> vertices = transaction.query().has("name", name).vertices();
      TitanVertex vertex = vertices.iterator().next();
      int age = vertex.value("age");
      return Results.ok(name + " is " + age + " year(s) old.\n");
    } finally {
      transaction.close();
    }
  }

  public Result getPersonsBetweenAges(int min, int max) {
    TitanTransaction transaction = titan.getGraph().newTransaction();
    try {
      GraphTraversalSource traversal = transaction.traversal();
      GraphTraversal<Vertex, String> names = traversal.V()
          .has("age", new P(Compare.gte, min))
          .has("age", new P(Compare.lte, max))
          .values("name");
      if (names.hasNext()) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("The following person(s) are between " + min + " and " + max + ": ");
        names.forEachRemaining(name -> buffer.append(name + ", "));
        return Results.ok(buffer.substring(0, buffer.length() - 2) + ".\n");
      } else {
        return Results.ok("No one is between " + min + " and " + max + ".\n");
      }
    } finally {
      transaction.close();
    }
  }

  public Result setEnemy(String name1, String name2, int strength) {
    createEdge(name1, name2, "enemy", strength);
    return Results.ok();
  }

  public Result setFriend(String name1, String name2, int strength) {
    createEdge(name1, name2, "friend", strength);
    return Results.ok();
  }

  private void createEdge(String name1, String name2, String label, int strength) {
    TitanTransaction transaction = titan.getGraph().newTransaction();
    try {
      Iterable<TitanVertex> vertices1 = transaction.query().has("name", name1).vertices();
      TitanVertex vertex1 = vertices1.iterator().next();
      Iterable<TitanVertex> vertices2 = transaction.query().has("name", name2).vertices();
      TitanVertex vertex2 = vertices2.iterator().next();
      vertex1.addEdge(label, vertex2, "strength", strength);
      transaction.commit();
    } finally {
      transaction.close();
    }
  }
}
