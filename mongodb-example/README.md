mongodb-example
=========

An example that uses [Avro](http://avro.apache.org/docs/1.7.7/spec.html) to define data schema and stores the data in [MongoDB](http://www.mongodb.org/).

---

#### Overview

MongoDB is a schemaless NoSQL storage. The [BSON](http://bsonspec.org/) data being stored in collections in MongoDB do not need to conform to any schema. The Java MongoDB driver provides methods to read and write the data in the form of generic records.

[Avro](http://avro.apache.org/), by contrast, is a way to specify schema. The schema may be used to verify data being transmitted between a client and a server in IPC (Inter-Process Communication). It helps detect malformed data. When the contract between the client and the server is extended to accommodate new business requirements, a new versions of the schema may be defined according to rules in the Avro specification. A client using a different version may communicate with the server, provided that the client-side schema and server-side schema are both available, and there is a conversion between the two.

In a use case where Avro IPC is adopted as the communication protocol and MongoDB is the storage, one may be tempted to maintain a set of Java classes for data manipulation (most commonly, through get and set methods). Those classes may be generated from Avro schemas, but the Avro-generated classes cannot be directly written to MongoDB, because the Avro Json format is slightly different from the MongoDB BSON.

The [MongoDB module](https://github.com/tfeng/toolbox/tree/master/mongodb) provides utilities to perform the necessary convertion and validation. With these utilities, this is how a developer defines his/her data structures and manipulates them:

1. The developer defines schemas for IPC in Avro IDL files (.avdl) or Avro protocol files (.avpr). The Avro IDL file for this example is [here](https://github.com/tfeng/play-mods-examples/blob/master/mongodb-example/schemata/points.avdl).
2. The developer adds [Play Avro module](https://github.com/tfeng/play-mods/tree/master/avro) to [project/plugins.sbt](https://github.com/tfeng/play-mods-examples/blob/master/mongodb-example/project/plugins.sbt), and add ```Avro.settings``` to [build.sbt](https://github.com/tfeng/play-mods-examples/blob/master/mongodb-example/build.sbt). When this is done, all the Avro files under the [schemata/](https://github.com/tfeng/play-mods-examples/blob/master/mongodb-example/schemata/) directory in the project are automatically compiled, and Java classes for the protocols and data types are automatically generated.
3. The developer writes the controller or reuses one of the predefined controllers in the Avro module. Avro data may be received in either the Avro binary format or the Avro Json format, as discussed in [avro-example](https://github.com/tfeng/play-mods-examples/blob/master/avro-example/).
4. MongoDB client should be set up with Spring wiring, as in [conf/spring/application-context.xml](https://github.com/tfeng/play-mods-examples/blob/master/mongodb-example/conf/spring/application-context.xml).
5. To write an Avro record into MongoDB, the developer invokes ```toDocument``` in [RecordConverter](https://github.com/tfeng/toolbox/blob/master/mongodb/src/main/java/me/tfeng/toolbox/mongodb/RecordConverter.java). To read an object in MongoDB back into an Avro record, ```toRecord``` may be used.

#### Running

Due to its dependency on MongoDB, this example requires extra steps to install MongoDB. Instructions to install MongoDB can be found [here](http://docs.mongodb.org/manual/installation/).

After MongoDB is installed, make sure it is running at localhost:27017. Port 27017 is the default. If MongoDB is installed to listen to a different port, the setting in [conf/mongodb.properties](https://github.com/tfeng/play-mods-examples/blob/master/mongodb-example/conf/mongodb.properties) must be adjusted accordingly.

To make sure MongoDB is up and running properly, run the following command:
```bash
$ mongo localhost:27017/test
MongoDB shell version: 3.0.6
connecting to: localhost:27017/test
>
```

Run the example with ```activator run```.

#### Manual testing

The example is similar to [avro-example](https://github.com/tfeng/play-mods-examples/blob/master/avro-example/). It computes _k_-nearest points from a given point.

At startup time, the application connects to MongoDB and clears the collection used to store data. All the objects in the "points" collection in "test" database are deleted.

When the application is running, one may add points, one at a time, through its Avro Json interface. The points are stored in MongoDB.
```bash
$ curl -X POST -H "Content-Type: avro/json" -d '{"point": {"x": 1.0, "y": 1.0}}' http://localhost:9000/points/addPoint
null

$ curl -X POST -H "Content-Type: avro/json" -d '{"point": {"x": -0.5, "y": -0.5}}' http://localhost:9000/points/addPoint
null

$ mongo localhost:27017/test
MongoDB shell version: 3.0.6
connecting to: localhost:27017/test
> use test
switched to db test
> db.points.find()
{ "_id" : ObjectId("5400397730042fdb3309bd48"), "x" : 1, "y" : 1 }
{ "_id" : ObjectId("540039e730042fdb3309bd49"), "x" : -0.5, "y" : -0.5 }
>
```

After some points are added, one may issue a request to compute the _k_-nearest points.
```bash
$ curl -X POST -H "Content-Type: avro/json" -d '{"from": {"x": 0, "y": 0}, "k": 2}' http://localhost:9000/points/getNearestPoints
[{"id":"540039e730042fdb3309bd49","x":-0.5,"y":-0.5},{"id":"5400397730042fdb3309bd48","x":1.0,"y":1.0}]
```

#### ID

MongoDB automatically generates an ID for each object. The ID is stored in the ```_id``` field. In an Avro schema, the ID may be represented by a different name. One my use ```@mongo-name("_id")``` to identify a field that should be mapped to the ```_id``` field in MongoDB. ```@mongo-type("OBJECT_ID")``` may also be used to define the field type to be ```ObjectId``` (the type to represent native IDs in MongoDB). See [points.avdl](https://github.com/tfeng/play-mods-examples/blob/master/mongodb-example/schemata/points.avdl) for an example. More ID examples can be found in [ids.avsc](https://github.com/tfeng/toolbox/blob/master/mongodb/src/test/resources/schemata/ids.avsc).

While adding a point to the server, the user may explicitly specify an ID.

```bash
$ curl -X POST -H "Content-Type: avro/json" -d '{"point": {"id": "5400397730042fdb3309bd48", "x": -0.5, "y": -0.5}}' http://localhost:9000/points/addPoint
null
$ curl -X POST -H "Content-Type: avro/json" -d '{"from": {"x": -0.5, "y": -0.5}, "k": 1}' http://localhost:9000/points/getNearestPoints
[{"id":"5400397730042fdb3309bd48","x":-0.5,"y":-0.5}]
```
