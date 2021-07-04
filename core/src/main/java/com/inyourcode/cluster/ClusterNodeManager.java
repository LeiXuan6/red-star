package com.inyourcode.cluster;

import com.alibaba.fastjson.JSONObject;
import com.inyourcode.cluster.api.ClusterConst;
import com.inyourcode.db.redis.RedisConfig;
import com.inyourcode.transport.api.UnresolvedAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集群状态管理
 */
@Component
public class ClusterNodeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterNodeManager.class);
    private ConcurrentHashMap<String, ClusterNodeConf> clusterNodeMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ClusterConnector> clusterConnectorMap = new ConcurrentHashMap<>();
    @Qualifier(RedisConfig.STRING_REDIS_TEMPLATE)
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    ClusterNodeConf currentClusterNodeConf;

    public void tick() {
        long currentTimeMillis = System.currentTimeMillis();
        currentClusterNodeConf.setLastActiveTimeMillis(currentTimeMillis);
        redisTemplate.opsForHash().put(ClusterConst.KEY_CLUSTER_DATA, currentClusterNodeConf.getNodeId(), JSONObject.toJSONString(currentClusterNodeConf));

        Map<String, String> clusterDataFromRedis = redisTemplate.opsForHash().entries(ClusterConst.KEY_CLUSTER_DATA);
        if (CollectionUtils.isEmpty(clusterDataFromRedis)) {
            return;
        }

        for (Map.Entry<String, String> entry : clusterDataFromRedis.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            ClusterNodeConf nodeFromDB = JSONObject.parseObject(value, ClusterNodeConf.class);

            String nodeId = nodeFromDB.getNodeId();
            if (nodeId.equals(currentClusterNodeConf.getNodeId())) {
                continue;
            }

            ClusterNodeConf existsNode = clusterNodeMap.get(nodeId);
            //新加入的节点
            if (existsNode == null) {
                clusterNodeMap.put(nodeId, nodeFromDB);
                LOGGER.info("New node[{}] is added to the cluster", nodeFromDB);
            } else {
                //检查是否存活
                long checkTime = currentTimeMillis - nodeFromDB.getLastActiveTimeMillis();
                if (checkTime > ClusterConst.CLUSTER_ACTIVE_TIME_MS) {
                    clusterNodeMap.remove(nodeId);
                    redisTemplate.opsForHash().delete(ClusterConst.KEY_CLUSTER_DATA, nodeId);
                    LOGGER.error("the node[{}] has lost connection.", nodeFromDB);
                } else {
                    clusterNodeMap.put(nodeId, nodeFromDB);
                }
            }

        }

        connectToOtherClusterNode();
    }

    private void connectToOtherClusterNode(){
        Collection<ClusterNodeConf> otherClusterNodes = clusterNodeMap.values();
        otherClusterNodes.forEach(otherClusterNode -> {
            if (!currentClusterNodeConf.getJoinClustTypes().contains(otherClusterNode.getNodeType())) {
                return;
            }

            String nodeId = otherClusterNode.getNodeId();
            String key = otherClusterNode.getNodeType() + "_" +nodeId;

            ClusterConnector clusterConnectorFromCache = clusterConnectorMap.get(key);
            if (clusterConnectorFromCache != null && clusterConnectorFromCache.hasConnected) {
                return;
            }

            ClusterNodeClient clusterNodeClient = new ClusterNodeClient(currentClusterNodeConf);
            clusterConnectorFromCache = new ClusterConnector();
            clusterConnectorFromCache.conf = otherClusterNode;
            clusterConnectorFromCache.hasConnected = true;
            clusterConnectorFromCache.clusterNodeClient = clusterNodeClient;

            ClusterConnector putIfAbsent = clusterConnectorMap.putIfAbsent(key, clusterConnectorFromCache);
            if (putIfAbsent != null) {
                return;
            }

            if (clusterConnectorFromCache.conf.hashCode() < currentClusterNodeConf.hashCode()) {
                return;
            }

            String[] split = currentClusterNodeConf.getClusterIp().split(":");
            String host = split[0];
            int port = Integer.valueOf(split[1]);
            clusterNodeClient.connect(new UnresolvedAddress(host, port), true);
        });
    }

    static class ClusterConnector {
        private ClusterNodeConf conf;
        private ClusterNodeClient clusterNodeClient;
        private volatile boolean hasConnected;

        public ClusterNodeClient getClusterNodeClient() {
            return clusterNodeClient;
        }

        public void setClusterNodeClient(ClusterNodeClient clusterNodeClient) {
            this.clusterNodeClient = clusterNodeClient;
        }

        public boolean isHasConnected() {
            return hasConnected;
        }

        public void setHasConnected(boolean hasConnected) {
            this.hasConnected = hasConnected;
        }

        public ClusterNodeConf getConf() {
            return conf;
        }

        public void setConf(ClusterNodeConf conf) {
            this.conf = conf;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClusterConnector that = (ClusterConnector) o;
            return Objects.equals(conf, that.conf);
        }

        @Override
        public int hashCode() {
            return Objects.hash(conf);
        }
    }
}


