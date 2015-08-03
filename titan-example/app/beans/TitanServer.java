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

import java.time.Duration;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.schema.TitanManagement;

import me.tfeng.playmods.spring.Startable;
import me.tfeng.playmods.titan.MongoDbStoreManager;
import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component
public class TitanServer implements Startable {

  private static final ALogger LOG = Logger.of(TitanServer.class);

  private TitanGraph graph;

  @Value("${ids.authority.wait-time}")
  private long idsAuthorityWaitTime;

  @Value("${ids.num-partitions}")
  private int idsNumPartitions;

  @Value("${index.search.backend}")
  private String indexSearchBackend;

  @Value("${index.search.directory}")
  private String indexSearchDirectory;

  public TitanGraph getGraph() {
    return graph;
  }

  @Override
  public void onStart() throws Throwable {
    graph = TitanFactory.build()
        .set("storage.backend", MongoDbStoreManager.class.getName())
        .set("index.search.backend", indexSearchBackend)
        .set("index.search.directory", indexSearchDirectory)
        .set("ids.num-partitions", idsNumPartitions)
        .set("ids.authority.wait-time", Duration.ofMillis(idsAuthorityWaitTime))
        .open();
    LOG.info("Titan graph opened");

    TitanManagement management = graph.openManagement();
    PropertyKey name = management.makePropertyKey("name").dataType(String.class).make();
    PropertyKey age = management.makePropertyKey("age").dataType(Integer.class).make();
    management.buildIndex("byName", Vertex.class).addKey(name).unique().buildCompositeIndex();
    management.buildIndex("byAge", Vertex.class).addKey(age).buildMixedIndex("search");
    management.commit();
    LOG.info("Titan graph index created");
  }

  @Override
  public void onStop() throws Throwable {
    graph.close();
  }
}
