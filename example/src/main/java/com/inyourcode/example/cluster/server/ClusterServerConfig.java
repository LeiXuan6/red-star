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
package com.inyourcode.example.cluster.server;

import com.inyourcode.cluster.ClusterService;
import com.inyourcode.cluster.ClusterNodeServer;
import com.inyourcode.cluster.ClusterQueryServer;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author JackLei
 */
@Configurable
@Configuration
@PropertySource("classpath:cluster-server.properties")
@EnableScheduling
@ComponentScan(basePackages = "${app.scanpackages}")
public class ClusterServerConfig {
    @Value("${cluster.server.ip}")
    private String ip;
    @Value("${cluster.server.port}")
    private int port;
    @Value(("${cluster.server.query.port}"))
    private int queryPort;

    @Bean
    public ClusterNodeServer clusterServer() throws InterruptedException {
        ClusterNodeServer clusterNodeServer = new ClusterNodeServer(port);
        clusterNodeServer.start(false);
        return clusterNodeServer;
    }

    @Bean
    public ClusterQueryServer queryServer(){
        ClusterQueryServer clusterQueryServer = new ClusterQueryServer(queryPort);
        clusterQueryServer.start();
        return clusterQueryServer;
    }

    @Scheduled(initialDelay = 5000, fixedRate = 1000)
    public void tick(){
        ClusterService.checkExpireNode();
        ClusterService.sortNode();
    }

}
