avro-d2-example
=========

An example for a scalable web service using [Avro IPC (Inter-Process Communication)](http://avro.apache.org/docs/current/spec.html#Protocol+Declaration) as the communication protocol and using [ZooKeeper](http://zookeeper.apache.org/) for client-side load balancing.

The design is based on [LinkedIn](https://www.linkedin.com)'s [rest.li](http://rest.li/), but is substantially different with respect to communication protocol.

---

#### Design

The computation power (and network bandwidth, and memory size, and storage capacity, ...) of a single machine is not without limit. The amount of requests, however, can increase without bound. For a web service to be scalable, it must be able to run on more machines to gain more power, as the business grows (hopefully). Therefore, while designing a service, one should think of it as a set of machines to begin with, rather than a single process.

##### Load balancing

For all machines in a service to serve the traffic in a fair manner, there must be some sort of load balancing. A simple solution is to adopt a load balancer, which routes traffic in a somewhat fair fashion to the participating machines. There are software load balancers, and there are hardware ones (which are generally faster but more expensive).

Another approach to load balancing is _client-side load balancing_. The clients (those who make requests to the service) route their requests directly to the machines. No intermediate load balancer relays those requests. The _cons_ of such an approach are:

1. clients need to know about the actual machines,
2. clients have additional logic, and
3. if clients behave incorrectly (e.g. keep sending enormous amount of traffic to the same machine), quality of the service may be impacted.

The _pros_ are:

* an extra hop on an intermediate load balancer is avoided,
* a single-point of failure is also avoided, and
* by knowing about the machines, clients may send traffic more intelligently (e.g., selecting machines based on bandwidth and geolocation).

If a client is trusted and can utilize a library that implements the client-side load balancing logic, then No. 2 and No. 3 in the cons are resolved. For No. 1, a mechanism is needed, which leads to the design below.

##### Dynamic Discovery

D2 in this document stands for _dynamic discovery_. It enables a client to dynamically discover machines that provide a service. The [Play Avro D2 module](https://github.com/tfeng/play-mods/tree/master/avro-d2) implements D2 by using ZooKeeper as a service registry.

###### Service registration

At service startup time, if the Avro D2 module is configured properly with the ZooKeeper connection strings and with the protocol information, it automatically registers the current machine in ZooKeeper.

In this example, the protocol is named Example in namespace controllers.protocols. It is defined in [example.avdl](https://github.com/tfeng/play-mods-examples/blob/master/avro-d2-example/schemata/example.avdl). The module first creates node ```/protocols/controllers.protocols.Example``` in ZooKeeper. (The operation is effectless if the node already exists.) It stores the URL to the service on the current machine in the ```servers``` child of that node. Moreover, since Avro allows a protocol to evolve into future versions (see [schema-evolution-example](https://github.com/tfeng/play-mods-examples/tree/master/schema-evolution-example) for more information), it also stores the schema in the ```versions``` child of that node. If there are multiple machines serving the same service, there will be multiple servers listed under ```servers```.

While inspecting the actual data stored in ZooKeeper using the ZooKeeper client, one may see the following.

```bash
$ zkCli.sh -server localhost:3181
...
[zk: localhost:3181(CONNECTED) 0] ls /protocols/controllers.protocols.Example
[servers, versions]
[zk: localhost:3181(CONNECTED) 1] ls /protocols/controllers.protocols.Example/servers
[0000000000]
[zk: localhost:3181(CONNECTED) 2] get /protocols/controllers.protocols.Example/servers/0000000000
http://localhost:9000/example
cZxid = 0x9
ctime = Tue Sep 08 22:16:07 PDT 2015
mZxid = 0x9
mtime = Tue Sep 08 22:16:07 PDT 2015
pZxid = 0x9
cversion = 0
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0x14fb08850a30000
dataLength = 29
numChildren = 0
[zk: localhost:3181(CONNECTED) 3] ls /protocols/controllers.protocols.Example/versions           
[557C06E1D8ABBCF9DBAEEB325414C177]
[zk: localhost:3181(CONNECTED) 4] get /protocols/controllers.protocols.Example/versions/557C06E1D8ABBCF9DBAEEB325414C177
{"protocol":"Example","namespace":"controllers.protocols","types":[],"messages":{"echo":{"request":[{"name":"message","type":{"type":"string","avro.java.string":"String"}}],"response":{"type":"string","avro.java.string":"String"}}}}
cZxid = 0x5
ctime = Tue Sep 08 22:16:07 PDT 2015
mZxid = 0x5
mtime = Tue Sep 08 22:16:07 PDT 2015
pZxid = 0x5
cversion = 0
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0x0
dataLength = 232
numChildren = 0
```

###### Client

When a client tries to send a request to the service, it must first obtain the URLs of the machines providing that service. It does this by retrieving the service information from ZooKeeper. If there are multiple machines listed under the ```servers``` node in ZooKeeper, the client may potentially send the request to any of the machines. The default client-side load balancing method is round-robin. Therefore, it is important that using Avro D2, machines providing a service not maintain local state. If no machine is found in ZooKeeper for the service, the request fails.

When a machine is disconnected (either by proper shutdown or because of a failure), it is automatically removed from ZooKeeper, and all the clients are notified. (The delay of notification depends on settings of ZooKeeper and network latency.) When this happens, the clients re-queries ZooKeeper to get the current list of machines. They also re-queries ZooKeeper if a new machine is added.

#### Manual testing

Run with ```activator run```.

When the application is started, it first creates a ZooKeeper server using a temporary directory as data storage. Then it registers its own URL with that ZooKeeper server.

There are two ways to send requests to this single-machine service:

1. sending Avro binary requests to its URL, and
2. using a client library, as described above, to dynamically discover the machine in ZooKeeper and to send requests to that machine.

##### Direct request

Using the service's URL, requests are sent essentially in the same way as not using D2. (See a similar Avro IPC example [here](https://github.com/tfeng/play-mods-examples/tree/master/avro-example).)

```bash
$ java -jar avro-tools-1.8.0.jar rpcsend http://localhost:9000/example codegen/example.avpr echo -data '{"message": "hello"}'
"hello"
```

##### Request with D2

The case with D2 is more interesting. The ```routes``` file defines another endpoint (/proxy). When requested at this endpoint, the application uses the client library to connect to ZooKeeper and to dynamically discover machines. It only finds itself (because the same machine is registered in ZooKeeper at the start of the application, as mentioned above). Therefore, the request is routed back to the same application, but at the /example endpoint. The user observes the same result.

```bash
$ curl "http://localhost:9000/proxy?message=hello"
hello
```
