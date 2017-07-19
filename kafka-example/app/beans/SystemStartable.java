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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.springframework.stereotype.Component;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import me.tfeng.toolbox.kafka.KafkaUtils;
import me.tfeng.toolbox.spring.Startable;
import play.Logger;
import play.Logger.ALogger;
import utils.Constants;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Component
public class SystemStartable implements Startable {

  private static final ALogger LOG = Logger.of(SystemStartable.class);

  private ServerCnxnFactory cnxnFactory;

  private File kafkaDirectory;

  private KafkaServerStartable kafkaServer;

  private File zkDirectory;

  private ZooKeeperServer zkServer;

  @Override
  public void onStart() throws Throwable {
    // Initialize ZooKeeper.
    Properties zkProperties = new Properties();
    InputStream propertiesStream = getClass().getClassLoader().getResourceAsStream("zoo.cfg");
    zkProperties.load(propertiesStream);

    QuorumPeerConfig quorumConfig = new QuorumPeerConfig();
    quorumConfig.parseProperties(zkProperties);

    zkServer = new ZooKeeperServer();
    zkServer.setTickTime(quorumConfig.getTickTime());
    zkServer.setMinSessionTimeout(quorumConfig.getMinSessionTimeout());
    zkServer.setMaxSessionTimeout(quorumConfig.getMaxSessionTimeout());

    zkDirectory = Files.createTempDirectory("zookeeper").toFile();
    LOG.info("Using ZooKeeper directory: " + zkDirectory);
    FileTxnSnapLog txnLog = new FileTxnSnapLog(zkDirectory, zkDirectory);
    zkServer.setTxnLogFactory(txnLog);

    cnxnFactory = ServerCnxnFactory.createFactory();
    cnxnFactory.configure(quorumConfig.getClientPortAddress(), quorumConfig.getMaxClientCnxns());
    cnxnFactory.startup(zkServer);

    // Initialize Kafka server
    kafkaDirectory = Files.createTempDirectory("kafka").toFile();
    LOG.info("Using Kafka directory: " + kafkaDirectory);
    Properties kafkaProperties = new Properties();
    propertiesStream = getClass().getClassLoader().getResourceAsStream("server.properties");
    kafkaProperties.load(propertiesStream);
    kafkaProperties.setProperty("log.dirs", kafkaDirectory.getAbsolutePath());
    KafkaConfig kafkaConfig = KafkaConfig.fromProps(kafkaProperties);
    kafkaServer = new KafkaServerStartable(kafkaConfig);
    kafkaServer.startup();

    // Create Kafka topic.
    int port = Integer.parseInt(zkProperties.getProperty("clientPort"));
    KafkaUtils.createTopic("localhost:" + port, Constants.TOPIC, 1, 1);
  }

  @Override
  public void onStop() throws Throwable {
    kafkaServer.shutdown();
    kafkaServer.awaitShutdown();
    try {
      FileUtils.deleteDirectory(kafkaDirectory);
    } catch (IOException e) {
      LOG.warn("Unable to delete Kafka directory: " + kafkaDirectory, e);
    }

    cnxnFactory.shutdown();
    zkServer.shutdown();
    try {
      FileUtils.deleteDirectory(zkDirectory);
    } catch (IOException e) {
      LOG.warn("Unable to delete ZooKeeper directory: " + zkDirectory, e);
    }
  }
}
