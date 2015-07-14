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

package factories;

import java.util.Map;

import org.apache.spark.SparkConf;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class SparkConfFactory implements FactoryBean<SparkConf> {

  private String appName;

  private String master;

  private Map<String, String> properties;

  @Override
  public SparkConf getObject() throws Exception {
    SparkConf conf = new SparkConf().setMaster(master).setAppName(appName);
    if (properties != null) {
      properties.forEach((k, v) -> conf.set(k, v));
    }
    return conf;
  }

  @Override
  public Class<?> getObjectType() {
    return SparkConf.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Required
  public void setAppName(String appName) {
    this.appName = appName;
  }

  @Required
  public void setMaster(String master) {
    this.master = master;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
}
