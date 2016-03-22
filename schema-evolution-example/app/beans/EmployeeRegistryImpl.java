/**
 * Copyright 2016 Thomas Feng
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

package beans;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import controllers.protocols.Employee;
import controllers.protocols.EmployeeRegistry;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("employeeRegistry")
public class EmployeeRegistryImpl implements EmployeeRegistry {

  private long currentEmployeeId = 0;

  private final Map<Long, Employee> employees = Maps.newHashMap();

  private final Multimap<Long, Long> management = ArrayListMultimap.create();

  @Override
  public long addEmployee(Employee employee) {
    employee.setId(++currentEmployeeId);
    employees.put(currentEmployeeId, employee);
    return currentEmployeeId;
  }

  @Override
  public int countEmployees() {
    return employees.size();
  }

  @Override
  public List<Employee> getEmployees(long managerId) {
    return management.get(managerId).stream().map(employees::get).collect(Collectors.toList());
  }

  @Override
  public Employee getManager(long employeeId) {
    return management.entries().stream()
        .filter(entry -> entry.getValue().equals(employeeId))
        .map(entry -> employees.get(entry.getKey()))
        .findAny()
        .orElse(null);
  }

  @Override
  public Void makeManager(long managerId, long employeeId) {
    removeEmployee(employeeId);
    management.put(managerId, employeeId);
    return null;
  }

  @Override
  public Void removeEmployee(long employeeId) {
    for (Iterator<Entry<Long, Long>> iterator = management.entries().iterator(); iterator.hasNext();) {
      if (iterator.next().getValue().equals(employeeId)) {
        iterator.remove();
      }
    }
    return null;
  }
}
