oauth2-avro-d2-example
=========

An example of Avro D2 web service with OAuth2, using [Avro D2 module](https://github.com/tfeng/play-mods/tree/master/avro-d2) and [OAuth2 module](https://github.com/tfeng/play-mods/tree/master/oauth2).

---

#### Overview

Avro D2 provides dynamic discovery and client-side load balancing. Similar to [Avro D2 example](../avro-d2-example), there is an endpoint in this example to accept Avro binary IPC requests. There is also an endpoint to simulate a client calling the Avro binary IPC endpoint with a generated Java proxy.

In addition to the two endpoints, this example exposes endpoints to authenticate a user with OAuth2. This is similar to [OAuth2 example](../oauth2-example). If an Avro request is not properly authenticated, the service returns an error.

#### Manual testing

The first step to interact with the service is to get a client access code.

```bash
$ curl -X POST -H "Content-Type: application/json" -d '{"clientId": "trusted-client", "clientSecret": "trusted-client-password"}' http://localhost:9000/client/authenticate
{"accessToken":"02db69c8-8bbc-4649-a033-e49088f9f5ce","clientId":"trusted-client","expiration":1409713005828}
```

With the client access code, one may authenticate a user with username and password.

```bash
$ curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer 02db69c8-8bbc-4649-a033-e49088f9f5ce" -d '{"username": "test", "password": "password"}' http://localhost:9000/user/authenticate
{"accessToken":"879fd16f-4b8a-4973-9f9d-27806f716fdc","username":"test","expiration":1409713028783,"refreshToken":"9c9ca53b-36c4-4677-adea-ebf518a21e86"}
```

Using the user access code, one may send a request to the /example endpoint and have a message echoed from the service.

```bash
$ curl -H "Authorization: Bearer 879fd16f-4b8a-4973-9f9d-27806f716fdc" "http://localhost:9000/proxy?message=hello"
hello
```

#### Error

If the user access token is not provided, the service returns status code ```401 Unauthorized```.

```bash
$ curl -i "http://localhost:9000/proxy?message=hello"
HTTP/1.1 401 Unauthorized
Content-Length: 0
```

If a client access token is mistakenly used in place of a user access token, the service also returns status code ```401 Unauthorized```.

```bash
$ curl -i -H "Authorization: Bearer 02db69c8-8bbc-4649-a033-e49088f9f5ce" "http://localhost:9000/proxy?message=hello"
HTTP/1.1 401 Unauthorized
Content-Length: 0
```

If Avro command-line tool ([avro-tools-1.7.7.jar](http://central.maven.org/maven2/org/apache/avro/avro-tools/1.7.7/avro-tools-1.7.7.jar)) is used to directly send a request to the service, an error is returned, because the command-line tool does not include the access token.

```bash
$ java -jar avro-tools-1.7.7.jar rpcsend http://localhost:9000/example target/schemata/example.avpr echo -data '{"message": "hello"}'
Exception in thread "main" java.io.IOException: Server returned HTTP response code: 401 for URL: http://localhost:9000/example
	at sun.net.www.protocol.http.HttpURLConnection.getInputStream0(HttpURLConnection.java:1838)
	at sun.net.www.protocol.http.HttpURLConnection.getInputStream(HttpURLConnection.java:1439)
	at org.apache.avro.ipc.HttpTransceiver.readBuffers(HttpTransceiver.java:54)
	at org.apache.avro.ipc.Transceiver.transceive(Transceiver.java:59)
	at org.apache.avro.ipc.Transceiver.transceive(Transceiver.java:72)
	at org.apache.avro.ipc.Requestor.request(Requestor.java:147)
	at org.apache.avro.ipc.Requestor.request(Requestor.java:101)
	at org.apache.avro.ipc.generic.GenericRequestor.request(GenericRequestor.java:58)
	at org.apache.avro.tool.RpcSendTool.run(RpcSendTool.java:101)
	at org.apache.avro.tool.Main.run(Main.java:84)
	at org.apache.avro.tool.Main.main(Main.java:73)
```

Currently, there is no way to send a request with the access token using the Avro command-line tool. [HttpTransceiver](http://avro.apache.org/docs/1.7.7/api/java/org/apache/avro/ipc/HttpTransceiver.html) in the Avro library does not set an authorization header. To see how this is done with a custom transceiver, refer to [InternalHttpTransceiver](https://github.com/tfeng/play-mods/blob/master/avro/app/org/apache/avro/ipc/InternalHttpTransceiver.java).
