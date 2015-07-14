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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import controllers.protocols.KTooLargeError;
import controllers.protocols.Point;
import controllers.protocols.Points;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("points")
public class PointsImpl implements Points {

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

  private final List<Point> points = new ArrayList<>();

  @Override
  public Void addPoint(Point point) {
    points.add(point);
    return null;
  }

  @Override
  public void clear() {
    points.clear();
  }

  @Override
  public List<Point> getNearestPoints(Point from, int k) throws KTooLargeError {
    if (points.size() < k) {
      throw KTooLargeError.newBuilder().setValue("k is too large").setK(k).build();
    }

    PriorityQueue<Point> queue = new PriorityQueue<>(new DescendingPointComparator(from));
    for (Point point : points) {
      queue.add(point);
      if (queue.size() > k) {
        queue.poll();
      }
    }
    List<Point> points = new ArrayList<>(queue.size());
    while (!queue.isEmpty()) {
      points.add(queue.poll());
    }
    return Lists.reverse(points);
  }
}
