/**
 * Copyright 2015 Thomas Feng
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.running;
import static play.test.Helpers.testServer;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class IntegrationTest {

  private static final int TIMEOUT = Integer.MAX_VALUE;

  @Test
  public void testAdd() {
    running(testServer(3333), () -> {
      try {
        WSResponse response;

        response = createPerson("thomas", 30);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = WS.url("http://localhost:3333/get")
            .setQueryParameter("name", "thomas")
            .get()
            .get(TIMEOUT);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("thomas is 30 year(s) old.\n"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testAgeQuery() {
    running(testServer(3333), () -> {
      try {
        WSResponse response;

        response = createPerson("amy", 30);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("brian", 35);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("catherine", 40);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("dave", 50);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = queryBetweenAges(10, 20);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("No one is between 10 and 20.\n"));

        response = queryBetweenAges(null, null);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(),
            is("The following person(s) are between 0 and 120: amy, brian, catherine, dave.\n"));

        response = queryBetweenAges(null, 30);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("The following person(s) are between 0 and 30: amy.\n"));

        response = queryBetweenAges(40, null);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("The following person(s) are between 40 and 120: catherine, dave.\n"));

        response = queryBetweenAges(50, 50);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("The following person(s) are between 50 and 50: dave.\n"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testFriends() {
    running(testServer(3333), () -> {
      try {
        WSResponse response;

        response = createPerson("amy", 12);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("brian", 13);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("catherine", 40);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("dave", 14);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = setFriend("amy", "brian", 5);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = getFriends("amy");
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("The following person(s) are friends of amy: brian.\n"));

        response = getFriends("brian");
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("The following person(s) are friends of brian: amy.\n"));

        response = setFriend("amy", "catherine", 10);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = getFriends("amy");
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("The following person(s) are friends of amy: catherine, brian.\n"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testMoreFriends() {
    running(testServer(3333), () -> {
      try {
        WSResponse response;

        response = createPerson("amy", 12);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("brian", 13);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("catherine", 40);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("dave", 14);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = setFriend("amy", "brian", 5);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = setFriend("brian", "catherine", 10);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = setFriend("brian", "dave", 20);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = getMoreFriends("amy");
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(),
            is("The following person(s) are more friends of amy: brian, dave, catherine.\n"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testEnemies() {
    running(testServer(3333), () -> {
      try {
        WSResponse response;

        response = createPerson("amy", 12);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("brian", 13);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("catherine", 40);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("dave", 14);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("emma", 15);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = setFriend("amy", "brian", 5);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = setFriend("brian", "catherine", 10);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = setEnemy("amy", "dave", 20);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = setEnemy("dave", "emma", 1);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = getMoreFriends("amy");
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(),
            is("The following person(s) are more friends of amy: brian, catherine, emma.\n"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testUniqueness () {
    running(testServer(3333), () -> {
      try {
        WSResponse response;

        response = createPerson("thomas", 30);
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), isEmptyString());

        response = createPerson("thomas", 30);
        assertThat(response.getStatus(), is(400));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private WSResponse createPerson(String name, int age) {
    return WS.url("http://localhost:3333/add")
        .setQueryParameter("name", name)
        .setQueryParameter("age", Integer.toString(age))
        .get()
        .get(TIMEOUT);
  }

  private WSResponse getFriends(String name) {
    return WS.url("http://localhost:3333/getFriends")
        .setQueryParameter("name", name)
        .get()
        .get(TIMEOUT);
  }

  private WSResponse getMoreFriends(String name) {
    return WS.url("http://localhost:3333/getMoreFriends")
        .setQueryParameter("name", name)
        .get()
        .get(TIMEOUT);
  }

  private WSResponse queryBetweenAges(Integer min, Integer max) {
    WSRequest request = WS.url("http://localhost:3333/betweenAges");
    if (min != null) {
      request = request.setQueryParameter("min", min.toString());
    }
    if (max != null) {
      request = request.setQueryParameter("max", max.toString());
    }
    return request.get().get(TIMEOUT);
  }

  private WSResponse setEnemy(String name1, String name2, int strength) {
    return WS.url("http://localhost:3333/setEnemy")
        .setQueryParameter("name1", name1)
        .setQueryParameter("name2", name2)
        .setQueryParameter("strength", Integer.toString(strength))
        .get()
        .get(TIMEOUT);
  }

  private WSResponse setFriend(String name1, String name2, int strength) {
    return WS.url("http://localhost:3333/setFriend")
        .setQueryParameter("name1", name1)
        .setQueryParameter("name2", name2)
        .setQueryParameter("strength", Integer.toString(strength))
        .get()
        .get(TIMEOUT);
  }
}
