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

package controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import controllers.protocols.ExampleClient;
import me.tfeng.playmods.avro.d2.AvroD2Component;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Results;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component
public class ProxyController {

  @Autowired
  @Qualifier("play-mods.avro-d2.component")
  private AvroD2Component avroD2Component;

  public Promise<Result> invoke(String message) throws Exception {
    ExampleClient proxy = avroD2Component.client(ExampleClient.class);
    return proxy.echo(message).map(response -> Results.ok(response.toString()));
  }
}
