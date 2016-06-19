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

package beans;

import java.util.concurrent.CompletionStage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import me.tfeng.toolbox.mongodb.OplogItem;
import me.tfeng.toolbox.mongodb.OplogItemHandler;
import play.Logger;
import play.Logger.ALogger;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component
public class StatusReporter implements OplogItemHandler {

  private static final ALogger LOG = Logger.of(StatusReporter.class);

  @Autowired
  private PointsImpl points;

  @Override
  public void handle(OplogItem oplogItem) {
    CompletionStage<Long> points = this.points.countPoints();
    CompletionStage<Double> pointsPerSecond = this.points.calculatePointsPerSecond();
    try {
      LOG.info("Storage status: " + points.toCompletableFuture().get() + " points; "
          + String.format("%.3f", pointsPerSecond.toCompletableFuture().get()) + " points/sec");
    } catch (Exception e) {
      LOG.error("Unable to get status", e);
    }
  }
}
