avro-d2-example
=========

An example for a scalable web service using [Avro IPC](http://avro.apache.org/docs/current/spec.html#Protocol+Declaration) as the communication protocol and using [ZooKeeper](http://zookeeper.apache.org/) for client-side load balancing.

The design is based on [LinkedIn](https://www.linkedin.com)'s [rest.li](http://rest.li/), but is substantially different with respect to communication protocol.

---

#### Design

The computation power (and network bandwidth, and memory size, and storage capacity, ...) of a single machine is not without limit. The amount of requests, however, can increase without bound. For a web service to be scalable, it must be able to run on more machines to gain more power, as the business grows (hopefully). Therefore, while designing a service, one should think of it as a set of machines to begin with, rather than a single process.

##### Load balancing

For all machines in a service to serve the traffic in a fair manner, there must be some sort of load balancing. A simple solution is by adopting a load balancer, which routes traffic in a somewhat fair fashion to the participating machines. There are software load balancers, and there are hardware ones (which are generally faster but more expensive).

Another approach to load balancing is _client-side load balancing_. When applied, the clients (those who make requests to the service) route their requests directly to the machines, rather than relaying on an intermediate load balancer. The _cons_ of such an approach are:

1. clients need to know about the actual machines,
2. clients have additional logic, and
3. if clients behave maliciously (e.g. keep sending enormous amount of traffic to the same machine), quality of the service may be impacted.

The _pros_ are:

* a hop is avoided by not introducing an extra layer in the network,
* a single-point of failure is also avoided, and
* by knowing about the machines, clients may send traffic more intelligently (e.g., selecting machines based on geolocation).

If a client is trusted and can utilize a library that implements the client-side load balancing logic, then No. 2 and No. 3 in the cons are addressed. For No. 1, a mechanism is needed.

##### Dynamic Discovery

D2 in this document stands for _dynamic discovery_. It enables a client to dynamically discover machines that provide a service. The [Play Avro D2 module](https://github.com/tfeng/play-mods/tree/master/avro-d2) implements D2 by using ZooKeeper as a registry.

###### Service registration

When a service application starts, if the Avro D2 module is configured properly with the ZooKeeper connection strings and with the protocol information, it automatically registers the machine in ZooKeeper.

In this example, the protocol is named Example (in [example.avdl](https://github.com/tfeng/play-examples/blob/master/avro-d2-example/schemata/example.avdl)), so the module first tries to create node /Example in ZooKeeper. (The operation is effectless if the node already exists.) Since Avro allows a protocol to evolve into future versions, the module creates a child node with the current protocol's MD5, /Example/c6e2843475fe402522b5cfb3c2d73006, to uniquely identify a specific version of a protocol. Under this node, a child node is created for each machine, so if there are multiple machines supporting the same protocol (with same name and same MD5), multiple children are listed.

The data of each child node specifies the actual URL to access the corresponding machine.

```bash
[zk: localhost:2181(CONNECTED) 0] get /Example/c6e2843475fe402522b5cfb3c2d73006/0000000000
http://localhost:9000/example
cZxid = 0x4
ctime = Thu Aug 21 23:16:48 PDT 2014
mZxid = 0x4
mtime = Thu Aug 21 23:16:48 PDT 2014
pZxid = 0x4
cversion = 0
dataVersion = 0
aclVersion = 0
ephemeralOwner = 0x147fc5c36320000
dataLength = 29
numChildren = 0
```

###### Client

When a client tries to send a request to the service, it must first obtain the URI of the service. The URI is in the form of "avsd://Example/c6e2843475fe402522b5cfb3c2d73006." Scheme "avsd" stands for "Avro Service Discovery." Following the scheme is the path in ZooKeeper. In order to construct the path, the client only requires the protocol. Note that this URI does not specify location of the ZooKeeper service, as it is assumed to be pre-configured.

With the URI, the client would look up the available machines. If there is no machine registered at this URI, the request to the service fails. Otherwise, the client chooses one of the machines in a round-robin manner. Once a machine is selected, the client directly communicates with that machine by using its actual URL, as stored in the ZooKeeper node.

If a machine is disconnected, its node is removed from ZooKeeper, and all the clients are notified. (The delay of notification depends on settings of ZooKeeper and network latency.) When this happens, the clients re-queries ZooKeeper to get the current list of machines.

#### Manual testing

Run with ```activator run```.

When the application is started, it first creates a ZooKeeper server using a temporary directory as data storage. Then it registers its own URL with that ZooKeeper server. Hence, there are two ways to send requests to this single-machine service:

1. sending Avro binary requests to its URL, and
2. using a client library, as described above, to dynamically discover the machine in ZooKeeper and to send requests to that machine.

##### Direct request

Using the service's URL, requests are sent essentially in the same way as not using D2. (See a similar Avro IPC example [here](https://github.com/tfeng/play-examples/tree/master/avro-example).)

```bash
$ java -jar avro-tools-1.7.7.jar rpcsend http://localhost:9000/example target/schemata/example.avpr echo -data '{"message": "hello"}'
"hello"
```

##### Request with D2

The case with D2 is more interesting. The ```routes``` file defines another endpoint (/proxy). When requested at this endpoint, the application uses the client library to connect to ZooKeeper and to dynamically discover machines. It only finds itself (because the same machine is registered in ZooKeeper at the start of the application, as mentioned above). Therefore, the request is routed back to the same application, but at the /example endpoint. The user observes the same result.

```bash
$ curl "http://localhost:9000/proxy?message=hello"
hello
```
