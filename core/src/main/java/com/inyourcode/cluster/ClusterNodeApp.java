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

import com.inyourcode.common.util.Preconditions;
import com.inyourcode.db.redis.RedisConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.UUID;

/**
 * @author JackLei
 */
@Configurable
@Configuration
@PropertySource("classpath:cluster-client.properties")
@EnableScheduling
@ComponentScan(basePackages = "${app.scanpackages}")
@Import(RedisConfig.class)
public class ClusterNodeApp {
    @Value("${cluster.node.join}")
    private String joinType;
    @Value("${cluster.node.type}")
    private String nodeType;
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
    @Autowired
    ClusterNodeManager clusterNodeManager;

    @Bean
    public ClusterNodeConf clusterNodeConf() {
        ClusterType clusterType = ClusterType.getType(nodeType);
        Preconditions.checkNotNull(clusterType, "cluster type not found, " + nodeType);

        ClusterNodeConf clusterNodeInfo = new ClusterNodeConf();
        clusterNodeInfo.setUuid(UUID.randomUUID().toString() + "_" + nodeId);
        clusterNodeInfo.setGroupId(clusterGroupId);
        clusterNodeInfo.setNodeId(nodeId);
        clusterNodeInfo.setNodeName(nodeName);
        clusterNodeInfo.setClusterIp(clusterIp);
        clusterNodeInfo.setMaxLoad(maxLoad);
        clusterNodeInfo.setNodeType(clusterType.getName());

        String[] split = joinType.split(",");
        for (String joinType : split) {
            clusterNodeInfo.getJoinClustTypes().add(joinType);
        }
        return clusterNodeInfo;
    }

    @Bean
    ClusterNodeServer clusterNodeServer(ClusterNodeConf clusterNodeConf) throws InterruptedException {
        String clusterIp = clusterNodeConf.getClusterIp();
        String[] split = clusterIp.split(":");
        int port = Integer.valueOf(split[1]);
        ClusterNodeServer clusterNodeServer = new ClusterNodeServer(port);
        clusterNodeServer.start(false);
        return clusterNodeServer;
    }


    @Scheduled(initialDelay = 5000, fixedRate = 1000)
    public void tick() {
        clusterNodeManager.tick();
    }

}

