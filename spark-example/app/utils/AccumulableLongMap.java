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

package utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.spark.AccumulableParam;

import com.google.common.collect.Maps;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
public class AccumulableLongMap<K> extends HashMap<K, Long>
    implements AccumulableParam<Map<K, Long>, Pair<K, Long>> {

  @Override
  public Map<K, Long> addAccumulator(Map<K, Long> map, Pair<K, Long> pair) {
    K key = pair.getLeft();
    if (map.containsKey(key)) {
      map.put(key, map.get(key) + pair.getRight());
    } else {
      map.put(key, pair.getRight());
    }
    return map;
  }

  @Override
  public Map<K, Long> addInPlace(Map<K, Long> map1, Map<K, Long> map2) {
    map2.forEach((k, v) -> {
      if (map1.containsKey(k)) {
        map1.put(k, map1.get(k) + v);
      } else {
        map1.put(k, v);
      }
    });
    return map1;
  }

  @Override
  public Map<K, Long> zero(Map<K, Long> initialValue) {
    return Maps.newHashMap();
  }
}
