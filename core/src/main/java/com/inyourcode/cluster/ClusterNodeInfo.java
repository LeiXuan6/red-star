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

import com.inyourcode.cluster.api.INodeType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author JackLei
 */
public class ClusterNodeInfo {
    private String groupId;
    private String uuid;
    private String nodeId;
    private String nodeName;
    private INodeType nodeType;
    private String nodeIp;
    private long reportTimeMillis;
    private int  currentLoad;
    private int maxLoad;

    private Map<String, String> extend = new HashMap<>();

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setNodeType(INodeType nodeType) {
        this.nodeType = nodeType;
    }

    public void setNodeIp(String nodeIp) {
        this.nodeIp = nodeIp;
    }

    public void setReportTimeMillis(long reportTimeMillis) {
        this.reportTimeMillis = reportTimeMillis;
    }

    public void setCurrentLoad(int currentLoad) {
        this.currentLoad = currentLoad;
    }

    public void setMaxLoad(int maxLoad) {
        this.maxLoad = maxLoad;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public INodeType getNodeType() {
        return nodeType;
    }

    public String getNodeIp() {
        return nodeIp;
    }

    public long getReportTimeMillis() {
        return reportTimeMillis;
    }

    public int getCurrentLoad() {
        return currentLoad;
    }

    public int getMaxLoad() {
        return maxLoad;
    }

    public Map<String, String> getExtend() {
        return extend;
    }


    @Override
    public String toString() {
        return "ClusterNodeInfo{" +
                "groupId='" + groupId + '\'' +
                ", uuid='" + uuid + '\'' +
                ", nodeId='" + nodeId + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", nodeType=" + nodeType +
                ", nodeIp='" + nodeIp + '\'' +
                ", reportTimeMillis=" + reportTimeMillis +
                ", currentLoad=" + currentLoad +
                ", maxLoad=" + maxLoad +
                ", extend=" + extend +
                '}';
    }
}
