dust-example
=========

An example to show how to render messages with [LinkedIn Dust.js](http://linkedin.github.io/dustjs/).

---

Run with ```activator run```.

This example uses [Play Dust module](https://github.com/tfeng/play-mods/tree/master/dust). The module compiles Dust templates (.js files) to JavaScripts in the compilation phase. At run time, those JavaScripts are loaded and rendered using either Java 8's [Nashorn](http://openjdk.java.net/projects/nashorn/) engine or standalone [Node.js](https://nodejs.org/) processes.

The server provides an endpoint to say "hello" to someone.

```bash
$ curl 'http://localhost:9000'
Hello, Thomas!

$ curl 'http://localhost:9000?name=Amy'
Hello, Amy!
```

For performance reason, as described above, Dust-to-JavaScript compilation happens at compile time. If one runs the server with ```activator start``` (in production mode), the compiled JavaScript files are included in the package, but not the Dust templates (.tl files).

For improved performance, one may execute JavaScripts with Node.js instead of Nashorn. Uncomment the properties in ```conf/dust.properties``` and set the correct path to Node.js. Execute ```activator run``` again.

```
$ curl 'http://localhost:9000'
Hello, Thomas!
$ curl 'http://localhost:9000?name=Amy'
Hello, Amy!
$ ps|grep node
39268 ttys000    0:00.10 /usr/local/bin/node -i
39269 ttys000    0:00.09 /usr/local/bin/node -i
39270 ttys000    0:00.09 /usr/local/bin/node -i
39271 ttys000    0:00.10 /usr/local/bin/node -i
39272 ttys000    0:00.09 /usr/local/bin/node -i
39274 ttys001    0:00.00 grep node
$ killall node
$ curl -i 'http://localhost:9000'
HTTP/1.1 400 Bad Request
Date: Wed, 09 Sep 2015 06:43:29 GMT
Content-Length: 0
```

In this case, 5 Node.js processes are created in a pool. They are all initialized with Dust.js. Given a request, the server picks one of the processes to execute the JavaScript. If all processes are killed, the request fails.
