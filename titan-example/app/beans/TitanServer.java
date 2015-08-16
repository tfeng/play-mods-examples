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

package beans;

import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mongodb.MongoClient;
import com.thinkaurelius.titan.core.EdgeLabel;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.schema.TitanManagement;

import me.tfeng.toolbox.spring.Startable;
import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component
public class TitanServer implements Startable {

  private static final ALogger LOG = Logger.of(TitanServer.class);

  @Value("${titan-example.db-name}")
  private String dbName;

  @Autowired
  @Qualifier("titan-example.titan-graph")
  private TitanGraph graph;

  @Autowired
  @Qualifier("titan-example.mongo-client")
  private MongoClient mongoClient;

  @Value("${titan-example.reset-data:false}")
  private boolean resetData;

  public TitanGraph getGraph() {
    return graph;
  }

  @Override
  public void onStart() throws Throwable {
    if (resetData) {
      mongoClient.getDatabase(dbName).drop();
    }

    LOG.info("Titan graph opened");

    TitanManagement management = graph.openManagement();

    PropertyKey name;
    if (graph.containsPropertyKey("name")) {
      name = graph.getPropertyKey("name");
    } else {
      name = management.makePropertyKey("name").dataType(String.class).make();
    }

    PropertyKey age;
    if (graph.containsPropertyKey("age")) {
      age = graph.getPropertyKey("age");
    } else {
      age = management.makePropertyKey("age").dataType(Integer.class).make();
    }

    PropertyKey strength;
    if (graph.containsPropertyKey("strength")) {
      strength = graph.getPropertyKey("strength");
    } else {
      strength = management.makePropertyKey("strength").dataType(Integer.class).make();
    }

    EdgeLabel friend;
    if (graph.containsEdgeLabel("friend")) {
      friend = graph.getEdgeLabel("friend");
    } else {
      friend = management.makeEdgeLabel("friend").signature(strength).make();
    }

    EdgeLabel enemy;
    if (graph.containsEdgeLabel("enemy")) {
      enemy = graph.getEdgeLabel("enemy");
    } else {
      enemy = management.makeEdgeLabel("enemy").signature(strength).make();
    }

    if (!management.containsGraphIndex("byName")) {
      management.buildIndex("byName", Vertex.class).addKey(name).unique().buildCompositeIndex();
    }

    if (!management.containsGraphIndex("byAge")) {
      management.buildIndex("byAge", Vertex.class).addKey(age).buildMixedIndex("search");
    }

    if (!management.containsRelationIndex(friend, "friendByStrength")) {
      management.buildEdgeIndex(friend, "friendByStrength", Direction.BOTH, Order.decr, strength);
    }

    if (!management.containsRelationIndex(enemy, "enemyByStrength")) {
      management.buildEdgeIndex(enemy, "enemyByStrength", Direction.BOTH, Order.decr, strength);
    }

    management.commit();
    LOG.info("Titan graph index created");
  }

  @Override
  public void onStop() throws Throwable {
    graph.close();
  }
}
