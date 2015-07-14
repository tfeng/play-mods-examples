avro-example
=========

A simple example that demonstrates how to create an [Avro IPC](http://avro.apache.org/docs/current/spec.html#Protocol+Declaration) server and exchange messages with it.

---

Run the server with ```activator run```.

The ```routes``` files contains 4 endpoints, all supporting only ```POST``` type of HTTP requests.
* /example and /points: These endpoints accept Avro binary requests, which should have ```Content-Type: avro/binary``` header.
    * /example endpoint provides a simple protocol that can echo text.
    * /points endpoint provides the functionality to calculate nearest points. It provides a method to add one 2-dimentional point at a time, a method to compute the _k_ nearest points from a given point, and a method to clear all the added points.
* /example/\* and /points/\*: These endpoints accept Avro Json requests, which should have ```Content-Type: avro/json``` header. The functionality is similar to the previous Avro binary endpoints, except that method name of a protocol is specifically names in the URL instead of a separate message parameter.

#### Sending request with Avro command-line tool

Avro command-line tool ([avro-tools-1.7.7.jar](http://central.maven.org/maven2/org/apache/avro/avro-tools/1.7.7/avro-tools-1.7.7.jar)) requires protocol (.avpr) files while sending requests to a server. Therefore, for this example, one must first generate the protocol files from the Avro IDL files (.avdl). Specification of Avro IDL can be found [here](http://avro.apache.org/docs/current/idl.html).

Run ```activator compile``` before ```activator run``` to have those protocol files generated.

##### /example

```bash
$ java -jar avro-tools-1.7.7.jar rpcsend http://localhost:9000/example target/schemata/example.avpr echo -data '{"message": "hello"}'
"hello"
```

##### /points

```bash
$ java -jar avro-tools-1.7.7.jar rpcsend http://localhost:9000/points target/schemata/points.avpr addPoint -data '{"point": {"x": 1.0, "y": 1.0}}'
null

$ java -jar avro-tools-1.7.7.jar rpcsend http://localhost:9000/points target/schemata/points.avpr addPoint -data '{"point": {"x": -0.5, "y": -0.5}}'
null

$ java -jar avro-tools-1.7.7.jar rpcsend http://localhost:9000/points target/schemata/points.avpr getNearestPoints -data '{"from": {"x": 0, "y": 0}, "k": 2}'
[ {
  "x" : -0.5,
  "y" : -0.5
}, {
  "x" : 1.0,
  "y" : 1.0
} ]

$ java -jar avro-tools-1.7.7.jar rpcsend http://localhost:9000/points target/schemata/points.avpr clear -data ''
null

$ java -jar avro-tools-1.7.7.jar rpcsend http://localhost:9000/points target/schemata/points.avpr getNearestPoints -data '{"from": {"x": 0, "y": 0}, "k": 2}'
Exception in thread "main" org.apache.avro.AvroRemoteException: {"k": 2}
	at org.apache.avro.ipc.generic.GenericRequestor.readError(GenericRequestor.java:101)
	at org.apache.avro.ipc.Requestor$Response.getResponse(Requestor.java:554)
	at org.apache.avro.ipc.Requestor$TransceiverCallback.handleResult(Requestor.java:359)
	at org.apache.avro.ipc.Requestor$TransceiverCallback.handleResult(Requestor.java:322)
	at org.apache.avro.ipc.Transceiver.transceive(Transceiver.java:73)
	at org.apache.avro.ipc.Requestor.request(Requestor.java:147)
	at org.apache.avro.ipc.Requestor.request(Requestor.java:101)
	at org.apache.avro.ipc.generic.GenericRequestor.request(GenericRequestor.java:58)
	at org.apache.avro.tool.RpcSendTool.run(RpcSendTool.java:101)
	at org.apache.avro.tool.Main.run(Main.java:84)
	at org.apache.avro.tool.Main.main(Main.java:73)
```

#### Sending request with curl

A request can also be sent to the server using curl on the command-line. We may use the Avro Json format, which encodes data in plain text and is generally easier to use. However, note that this approach is not specified in Avro specification, so it is not a standard way. Also due to the significantly increased data size, one should use Avro Json only for testing purpose.

##### /example

```bash
$ curl -X POST -H "Content-Type: avro/json" -d '{"message": "hello"}' http://localhost:9000/example/echo
"hello"
```

##### /points

```bash
$ curl -X POST -H "Content-Type: avro/json" -d '{"point": {"x": 1.0, "y": 1.0}}' http://localhost:9000/points/addPoint
null

$ curl -X POST -H "Content-Type: avro/json" -d '{"point": {"x": -0.5, "y": -0.5}}' http://localhost:9000/points/addPoint
null

$ curl -X POST -H "Content-Type: avro/json" -d '{"from": {"x": 0, "y": 0}, "k": 2}' http://localhost:9000/points/getNearestPoints
[{"x":-0.5,"y":-0.5},{"x":1.0,"y":1.0}]

$ curl -X POST -H "Content-Type: avro/json" http://localhost:9000/points/clear
null

$ curl -X POST -H "Content-Type: avro/json" -d '{"from": {"x": 0, "y": 0}, "k": 2}' http://localhost:9000/points/getNearestPoints
{"controllers.protocols.KTooLargeError":{"k":2}}
```

One may observe that the response to the last curl request is not exactly the same as that to the last request made by Avro command-line tool. The reason is the lack of a standard way to respond with an exception, so the [Avro Play module](https://github.com/tfeng/play-mods/tree/master/avro) goes ahead and implements a custom protocol. The status code of the response in this case is ```HTTP/1.1 400 Bad Request``` instead of ```HTTP/1.1 200 OK``` (the status code for all Avro binary requests, as specified in Avro documentation). The data in the Avro Json response includes the exception class and the data.
