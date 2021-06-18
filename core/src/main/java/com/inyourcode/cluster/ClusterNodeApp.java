/*
 * Copyright (c) 2020 The red-star Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.inyourcode.cluster;

import com.inyourcode.cluster.api.ClusterType;
import com.inyourcode.cluster.api.JClusterClient;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Random;
import java.util.UUID;

/**
 * @author JackLei
 */
@Configurable
@Configuration
@PropertySource("classpath:cluster-client.properties")
@EnableScheduling
@ComponentScan(basePackages = "${app.scanpackages}")
public class ClusterNodeApp {
    @Value("${cluster.group.id}")
    private String clusterGroupId;
    @Value("${cluster.node.id}")
    private String nodeId;
    @Value("${cluster.node.name}")
    private String nodeName;
    @Value("${cluster.node.ip}")
    private String clusterIp;
    @Value("${cluster.node.maxLoad}")
    private int maxLoad;

    @Bean
    public ClusterNodeConf clusterNodeConf() {
        ClusterNodeConf clusterNodeInfo = new ClusterNodeConf();
        clusterNodeInfo.setUuid(UUID.randomUUID().toString() + "_" + nodeId);
        clusterNodeInfo.setGroupId(clusterGroupId);
        clusterNodeInfo.setNodeId(nodeId);
        clusterNodeInfo.setNodeName(nodeName);
        clusterNodeInfo.setClusterIp(clusterIp);
        clusterNodeInfo.setMaxLoad(maxLoad);
        clusterNodeInfo.setNodeType(ClusterType.LOBBY);
        return clusterNodeInfo;
    }

    @Bean
    ClusterNodeServer clusterNodeServer(ClusterNodeConf clusterNodeConf) {
        ClusterNodeServer clusterNodeServer = new ClusterNodeServer(clusterNodeConf.get);
        clusterNodeServer.bind()
    }

    @Bean
    JClusterClient clusterClient(ClusterNodeConf clusterNodeConf) {
        ClusterNodeClient clusterNodeClient = new ClusterNodeClient(ClusterType.LOBBY, clusterNodeConf);
        return clusterNodeClient;
    }

    @Scheduled(initialDelay = 5000, fixedRate = 1000)
    public void tick() {
        Random random = new Random();
        int randomLoad = random.nextInt(1000);

    }
//
//    @Scheduled(initialDelay = 6000, fixedRate = 2000)
//    public void forwardMessage(){
//        clusterClient.broadcastMessageToCluster("hello gays");
//    }
//
//    @Scheduled(initialDelay = 7000, fixedRate = 4000)
//    public void sendMessageToCenter(){
//        clusterClient.connectToCluster("hello center");
//    }
//
//    @Scheduled(initialDelay = 7000, fixedRate = 6000)
//    public void sendMessageToCluster(){
//        HashSet clusterNodeIdSet = new HashSet();
//        clusterNodeIdSet.add("3");
//        clusterClient.sendMessageToCluster("hello cluster[3]", clusterNodeIdSet);
//    }
}

