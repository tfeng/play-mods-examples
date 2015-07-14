kafka-example
=========
An example to demonstrate how to produce and consume events using [Apache Kafka](http://kafka.apache.org/).

---

#### Overview

Kafka is a scalable messaging framework. Publishers and subscribers produce and consume messages through brokers. The brokers, producers and consumers register themselves in [ZooKeeper](http://zookeeper.apache.org/).

For a producer to produce messages, a topic must be created. The producer produces messages to the brokers under that topic. The brokers maintain recent messages (usually for several days), whereas older messages are deleted. A consumer receives messages under the same topic from the brokers.

Kafka is fault-tolerant. The messages may be parititioned. Multiple copies may exist for a partition. If a broker is down, the system may still function with other remaining brokers; if a consumer is down, message consumption may resume in other consumers in the same group, or the faulty consumer may resume the consumption from previous checkpoint at a later time.

This example is a simple use case of Kafka. An IPC (inter-process communication) server is set up to receive user inputs with either Avro binary protocol or Avro Json protocol (see [Avro example](https://github.com/tfeng/play-mods-examples/tree/master/avro-example) for more information on Avro IPC). When the user issues a request to the server, the request data is sent in a Kafka event. A consumer in the example consumes the message, and prints it out to the console.

#### Manual testing

Run with ```activator run```.

When the application is started, it first creates a ZooKeeper server and a Kafka Server using two temporary directories as data storage. Then it creates the topic ```kafka-example```, under which the producer is going to produce Kafka events.

The user may issue a request and observe a Kafka event being logged in the console of the server.

```bash
$ curl -X POST -H "Content-Type: avro/json" -d '{"message": {"subject": "Raymond", "action": "reads", "object": "books"}}' http://localhost:9000/message/send
null
```

(```null``` in the result simply means the request causes no return value.)

The following lines are in the server log, indicating a message was produced and consumed correctly.

```
[info] b.MessageImpl - Producing message: {"subject": "Raymond", "action": "reads", "object": "books", "requestHeader": {"remoteAddress": "0:0:0:0:0:0:0:1", "host": "localhost:9000", "method": "POST", "path": "/message/send", "query": {}, "secure": false, "timestamp": 1436762612193}}
...
[info] b.ConsumerStartable$ConsumerRunnable - Consuming message: {"subject": "Raymond", "action": "reads", "object": "books", "requestHeader": {"remoteAddress": "0:0:0:0:0:0:0:1", "host": "localhost:9000", "method": "POST", "path": "/message/send", "query": {}, "secure": false, "timestamp": 1436762612193}}
```
