package com.inyourcode.cluster;

import com.inyourcode.cluster.api.IClusterNodeType;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClusterService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterService.class);
    private static final long EXPIRE_TIME_MILLIS = 5 * 1000L;
    private static final ConcurrentHashMap<String, ClusterChannel> NODE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<IClusterNodeType, ClusterTypeNode> NODE_TYPE_MAP = new ConcurrentHashMap<>();

    protected static void registerClusterNode(Channel channel, ClusterNodeConf clusterNodeInfo) {
        String nodeId = clusterNodeInfo.getNodeId();
        IClusterNodeType nodeType = clusterNodeInfo.getNodeType();

        //缓存ClusterChannel
        ClusterChannel clusterChannel = NODE_MAP.get(nodeId);
        if (clusterChannel == null) {
            clusterChannel = new ClusterChannel(clusterNodeInfo, channel);
            NODE_MAP.put(nodeId, clusterChannel);
        } else {
            clusterChannel.channel = channel;
            clusterChannel.clusterNodeInfo = clusterNodeInfo;
        }

        //缓存ClusterTypeNode
        ClusterTypeNode clusterTypeNode = NODE_TYPE_MAP.get(nodeType);
        if (clusterTypeNode == null) {
            clusterTypeNode = new ClusterTypeNode(nodeType);
            NODE_TYPE_MAP.put(nodeType, clusterTypeNode);
        }
        clusterTypeNode.clusterNodeMap.put(nodeId, clusterNodeInfo);
        if (clusterTypeNode.minLoadNode == null) {
            clusterTypeNode.minLoadNode = clusterNodeInfo;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("register cluster node, node:{}", clusterNodeInfo);
        }
    }

    public static ClusterNodeConf select(IClusterNodeType nodeType) {
        ClusterTypeNode clusterTypeNode = NODE_TYPE_MAP.get(nodeType);
        if (clusterTypeNode == null) {
            return null;
        }
        return clusterTypeNode.minLoadNode;
    }

    public static void sortNode() {
        if (NODE_TYPE_MAP.isEmpty()) {
            return;
        }
        NODE_TYPE_MAP.forEach((type, node) -> {
            ArrayList<ClusterNodeConf> tempList = new ArrayList(node.clusterNodeMap.values());
            if (tempList.isEmpty()) {
                return;
            }
            Collections.sort(tempList, new LoadComparator());
            node.minLoadNode = tempList.get(0);
        });
    }

    public static void checkExpireNode() {
        Collection<ClusterChannel> values = NODE_MAP.values();
        ArrayList<ClusterChannel> nodeInfos = new ArrayList<>(values);
        long currentTimeMillis = System.currentTimeMillis();
        for (ClusterChannel clusterChannel : nodeInfos) {
            ClusterNodeConf clusterNodeInfo =  clusterChannel.clusterNodeInfo;
            if (clusterNodeInfo.getReportTimeMillis() + EXPIRE_TIME_MILLIS <= currentTimeMillis) {
                LOGGER.error("check expire node, node = {}", clusterNodeInfo.getNodeName());
                removeNode(clusterNodeInfo.getNodeId());
                NODE_MAP.remove(clusterNodeInfo.getNodeId());
            }
        }
    }

    private static void removeNode(String nodeId) {
        ClusterChannel remove = NODE_MAP.remove(nodeId);
        if (remove != null) {
            IClusterNodeType nodeType = remove.clusterNodeInfo.getNodeType();
            String removeNodeId = remove.clusterNodeInfo.getNodeId();
            ClusterTypeNode clusterTypeNode = NODE_TYPE_MAP.get(nodeType);
            if (clusterTypeNode != null) {
                clusterTypeNode.clusterNodeMap.remove(removeNodeId);
                //等待下次排序，重新选出minLoadNode
                if (clusterTypeNode.minLoadNode != null && removeNodeId.equals(clusterTypeNode.minLoadNode.getNodeId())) {
                    clusterTypeNode.minLoadNode = null;
                }
            }

        }
    }

    /**
     * 转发集群节点消息
     * @param srcChannel 原消息节点
     * @param obj 转发消息
     */
    protected static void forwardClusterMessage(Channel srcChannel, ClusterMessage obj) {
        Set<String> clusterNodeIds = obj.getTargetNodeIds();
        int forwardCount = 0;
        if (CollectionUtils.isEmpty(clusterNodeIds)){
            //转发给所有的ClusterNode
            Collection<ClusterChannel> clusterChannels = NODE_MAP.values();
            for(ClusterChannel clusterChannel : clusterChannels){
                clusterChannel.channel.writeAndFlush(obj);
            }

            forwardCount = NODE_MAP.size();
        }else{
            //转发到指定的ClusterNode
            for (String clusterNodeId : clusterNodeIds) {
                ClusterChannel clusterChannel = NODE_MAP.get(clusterNodeId);
                if (clusterChannel == null){
                    LOGGER.error("forward cluster message error, cluster node is null, node Id = {}", clusterNodeId);
                    continue;
                }

                clusterChannel.channel.writeAndFlush(obj);
            }

            forwardCount = clusterNodeIds.size();
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("forward the message to [{}] nodes, sourceChannel:{}, ", forwardCount, srcChannel);
        }

    }

    static class LoadComparator implements Comparator<ClusterNodeConf> {

        @Override
        public int compare(ClusterNodeConf o1, ClusterNodeConf o2) {
            if (o1.getCurrentLoad() > o2.getCurrentLoad()) {
                return 1;
            } else if (o1.getCurrentLoad() < o2.getCurrentLoad()) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    static class ClusterChannel {
        ClusterNodeConf clusterNodeInfo;
        Channel channel;

        public ClusterChannel(ClusterNodeConf clusterNodeInfo, Channel channel) {
            this.clusterNodeInfo = clusterNodeInfo;
            this.channel = channel;
        }
    }

    static class ClusterTypeNode {
        IClusterNodeType nodeType;
        ClusterNodeConf minLoadNode;
        Map<String, ClusterNodeConf> clusterNodeMap = new ConcurrentHashMap<>();

        public ClusterTypeNode(IClusterNodeType nodeType) {
            this.nodeType = nodeType;
        }
    }

    static class ClusterGroup{
        String group;

    }
}
