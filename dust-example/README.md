dust-example
=========

An example to show how to use [LinkedIn Dust.js](http://linkedin.github.io/dustjs/) as a rendering engine in Play.

---

Run with ```activator run```.

This example uses [Play Dust module](https://github.com/tfeng/play-mods/tree/master/dust). The module compiles Dust templates (.js files) to JavaScripts in the compilation phase. At run time, those JavaScripts are loaded and rendered using Java 8's [Nashorn](http://openjdk.java.net/projects/nashorn/) engine.

The server provides an endpoint to say "hello" to someone.

```bash
$ curl 'http://localhost:9000'
Hello, Thomas!

$ curl 'http://localhost:9000?name=Amy'
Hello, Amy!
```

For performance reason, as described above, Dust-to-JavaScript compilation happens at compile time. If one runs the server with ```activator start``` (in production mode), the compiled JavaScript files are included in the package, but not the Dust templates (.tl files).

For further improvement in performance, one may consider adding a rendering layer powered by [Node.js](http://nodejs.org/) to execute the JavaScripts, rather than using Nashorn.
