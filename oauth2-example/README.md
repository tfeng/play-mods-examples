oauth2-example
=========

An example for web service with OAuth2 support based on [Spring Security](http://projects.spring.io/spring-security/) and [Spring Security OAuth](http://projects.spring.io/spring-security-oauth/).

---

#### Overview

Security is important, as many developers know. What is also important is that there be an intuitive approach for them to write secured code and to verify their authorization logic. [OAuth2](http://oauth.net/2/) provides a standard way for a web service to authenticate and authorize users. Spring Security provides a framework. Spring Security OAuth provides integration between Spring Security and OAuth2. All is great.

Play framework, however, does not provide a standard way to develop secured code. The [Play OAuth2 module](https://github.com/tfeng/play-mods/tree/master/oauth2) fills this gap. It integrates Play application with Spring Security OAuth, and supports definition of authorization rules by Java annotation.

The module also provides an authentication controller. It implements the commonly required endpoints for authenticating a client and authenticating a user through that client. User of the module may customize the authentication with Spring wiring.

#### Manual Testing

A client must be authenticated by the service before it can send further requests to the service. The authentication specifies the privileges of the client (e.g., read-only, read-write, admin, etc). In this case, we test with curl on the command line, and we are acting as a client ourselves.

```bash
$ curl -X POST -H "Content-Type: application/json" -d '{"clientId": "trusted-client", "clientSecret": "trusted-client-password"}' http://localhost:9000/client/authenticate
{"accessToken":"f68570b0-0260-45ca-91db-1af37e6b6c73","clientId":"trusted-client","expiration":1409560020626}
```

Here the ```accessToken``` is the token that subsequent requests from the same client should use to authenticate itself with the same service.

In this example, for simplicity, the client ID and secret are predefined. All the access tokens are store in memory. After restart of the application, the previous access tokens are lost, and clients need to be re-authenticated.

If the authentication does not succeed, an error is returned.

```bash
$ curl -i -X POST -H "Content-Type: application/json" -d '{"clientId": "trusted-client", "clientSecret": "wrong-trusted-client-password"}' http://localhost:9000/client/authenticate
HTTP/1.1 401 Unauthorized
Content-Length: 0
Date: Tue, 22 Mar 2016 10:18:52 GMT
```

To authenticate a user, one would use the client access token and a valid username and password (predefined in this example).

```bash
$ curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer f68570b0-0260-45ca-91db-1af37e6b6c73" -d '{"username": "test", "password": "password"}' http://localhost:9000/user/authenticate
{"accessToken":"3e83b258-0bd0-4a5a-97b3-2669bb759c8f","username":"test","expiration":1409560883642,"refreshToken":"f5feb12c-6a00-4a9b-83d5-1a19b19b0d50"}
```

The ```accessToken``` returned this time can be used to authenticate the user. With this, a subsequence request can retrieve the user details.

```bash
$ curl -H "Authorization: Bearer 3e83b258-0bd0-4a5a-97b3-2669bb759c8f" http://localhost:9000/user/get
{"username":"test","isActive":true}
```

A user access token may expire. The above token has ```expiration``` being ```1409560883642```, which means "08/22/2014 8:49:21 GMT." After that time, the access token becomes invalid. To get a new valid access token, one must use the refresh token, in combination with a valid user access token or client access token.

```bash
$ curl -X POST -H "Content-Type: application/json" -H "Authorization: Bearer 3e83b258-0bd0-4a5a-97b3-2669bb759c8f" -d '{"refreshToken": "f5feb12c-6a00-4a9b-83d5-1a19b19b0d50"}' http://localhost:9000/user/refresh
{"accessToken":"82d9765e-2f7d-44b9-8697-20956b693e28","expiration":1409561622130,"refreshToken":"f5feb12c-6a00-4a9b-83d5-1a19b19b0d50"}
```

After refreshing, a new user access token is granted. Requests to retrieve user details should then be made with this new access token. The previous one becomes invalid. The same refresh token, however, can be used again and again.

Client access token ```f68570b0-0260-45ca-91db-1af37e6b6c73``` will also expire after its ```expiration``` time.
