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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.bson.Document;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoCollection;

import controllers.protocols.KTooLargeError;
import controllers.protocols.Point;
import controllers.protocols.PointsClient;
import me.tfeng.toolbox.mongodb.RecordConverter;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("points")
public class PointsImpl implements InitializingBean, PointsClient {

  private static class DescendingPointComparator implements Comparator<Point> {

    private final Point from;

    public DescendingPointComparator(Point from) {
      this.from = from;
    }

    @Override
    public int compare(Point point1, Point point2) {
      return - Double.compare(distanceSquare(from, point1), distanceSquare(from, point2));
    }

    private double distanceSquare(Point point1, Point point2) {
      double xDistance = point1.getX() - point2.getX();
      double yDistance = point1.getY() - point2.getY();
      return xDistance * xDistance + yDistance * yDistance;
    }
  }

  private MongoCollection<Document> collection;

  @Value("${mongodb-example.db-collection}")
  private String dbCollection;

  @Value("${mongodb-example.db-name}")
  private String dbName;

  @Autowired
  private MongoClient mongoClient;

  private long startTime;

  @Override
  public CompletionStage<Void> addPoint(Point point) {
    MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(dbCollection);
    CompletableFuture<Void> future = new CompletableFuture<>();
    collection.insertOne(RecordConverter.toDocument(point), getSingleResultCallback(future));
    return future;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    collection = mongoClient.getDatabase(dbName).getCollection(dbCollection);
    startTime = System.currentTimeMillis();
    clear();
  }

  @Override
  public CompletionStage<Void> clear() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    collection.drop(getSingleResultCallback(future));
    return future;
  }

  @Override
  public CompletionStage<List<Point>> getNearestPoints(Point from, int k) {
    CompletableFuture<Void> checkCountFuture = new CompletableFuture<>();
    collection.count((count, throwable) -> {
      if (throwable == null) {
        if (count < k) {
          checkCountFuture.completeExceptionally(KTooLargeError.newBuilder().setValue("k is too large").setK(k).build());
        } else {
          checkCountFuture.complete(null);
        }
      } else {
        checkCountFuture.completeExceptionally(throwable);
      }
    });

    return checkCountFuture.thenCompose(nothing -> {
      CompletableFuture<Void> insertQueueFuture = new CompletableFuture<>();
      PriorityQueue<Point> queue = new PriorityQueue<>(new DescendingPointComparator(from));
      collection.find()
          .map(document -> RecordConverter.toRecord(Point.class, document))
          .forEach(point -> {
            queue.add(point);
            if (queue.size() > k) {
              queue.poll();
            }
          }, getSingleResultCallback(insertQueueFuture));

      return insertQueueFuture.thenApply(nothing2 -> {
        List<Point> points = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) {
          points.add(queue.poll());
        }
        return Lists.reverse(points);
      });
    });
  }

  protected CompletionStage<Double> calculatePointsPerSecond() {
    long current = System.currentTimeMillis();
    return countPoints().thenApply(points -> (double) points * 1000 / (current - startTime));
  }

  protected CompletionStage<Long> countPoints() {
    CompletableFuture<Long> future = new CompletableFuture<>();
    collection.count(getSingleResultCallback(future));
    return future;
  }

  private <T> SingleResultCallback<T> getSingleResultCallback(CompletableFuture<T> future) {
    return (result, throwable) -> {
      if (throwable == null) {
        future.complete(result);
      } else {
        future.completeExceptionally(throwable);
      }
    };
  }
}
