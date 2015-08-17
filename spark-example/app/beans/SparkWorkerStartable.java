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

package beans;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.deploy.worker.Worker;
import org.apache.spark.deploy.worker.WorkerArguments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import akka.actor.ActorSystem;
import me.tfeng.toolbox.spring.Startable;
import play.Logger;
import play.Logger.ALogger;
import scala.Option;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component("sparkWorkerStartable")
@DependsOn("sparkMasterStartable")
public class SparkWorkerStartable implements Startable {

  private static final ALogger LOG = Logger.of(SparkWorkerStartable.class);

  private ActorSystem actorSystem;

  @Autowired
  private SparkConf sparkConf;

  @Value("${spark.host}")
  private String sparkHost;

  @Value("${spark.slave.port}")
  private int sparkSlavePort;

  @Value("${spark.slave.webui.port}")
  private int sparkSlaveWebUIPort;

  private File tempDir;

  @Override
  public void onStart() throws Throwable {
    WorkerArguments workerArguments = new WorkerArguments(new String[] {"spark://localhost:7077"}, sparkConf);
    tempDir = Files.createTempDirectory("sparkWorker").toFile();
    actorSystem = Worker.startSystemAndActor(sparkHost, sparkSlavePort, sparkSlaveWebUIPort, workerArguments.cores(),
        workerArguments.memory(), workerArguments.masters(), tempDir.toString(), Option.apply(1), sparkConf)._1();
  }

  @Override
  public void onStop() throws Throwable {
    actorSystem.shutdown();
    try {
      FileUtils.deleteDirectory(tempDir);
    } catch (IOException e) {
      LOG.warn("Unable to delete ZooKeeper directory: " + tempDir, e);
    }
  }
}
