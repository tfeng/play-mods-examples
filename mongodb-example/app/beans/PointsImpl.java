package beans;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.bson.Document;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

import controllers.protocols.KTooLargeError;
import controllers.protocols.Point;
import controllers.protocols.Points;
import me.tfeng.toolbox.mongodb.RecordConverter;

@Component("points")
public class PointsImpl implements InitializingBean, Points {

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
  public Void addPoint(Point point) {
    MongoCollection<Document> collection =
        mongoClient.getDatabase(dbName).getCollection(dbCollection);
    collection.insertOne(RecordConverter.toDocument(point));
    return null;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    collection = mongoClient.getDatabase(dbName).getCollection(dbCollection);
    startTime = System.currentTimeMillis();
    clear();
  }

  @Override
  public void clear() {
    collection.drop();
  }

  @Override
  public List<Point> getNearestPoints(Point from, int k) throws KTooLargeError {
    if (collection.count() < k) {
      throw KTooLargeError.newBuilder().setValue("k is too large").setK(k).build();
    }

    FindIterable<Document> cursor = collection.find();
    PriorityQueue<Point> queue = new PriorityQueue<>(new DescendingPointComparator(from));
    for (Document object : cursor) {
      Point point = RecordConverter.toRecord(Point.class, object);
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

  protected double calculatePointsPerSecond() {
    long current = System.currentTimeMillis();
    long points = countPoints();
    return (double) points * 1000 / (current - startTime);
  }

  protected long countPoints() {
    return collection.count();
  }
}
