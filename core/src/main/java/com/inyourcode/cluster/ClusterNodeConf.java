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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author JackLei
 */
public class ClusterNodeConf {
    private String groupId;
    private String uuid;
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private List<String> joinClustTypes = new ArrayList<>();
    private String clusterIp;
    private long reportTimeMillis;
    private int  currentLoad;
    private int maxLoad;
    private long lastActiveTimeMillis;
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

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public void setClusterIp(String clusterIp) {
        this.clusterIp = clusterIp;
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

    public String getNodeType() {
        return nodeType;
    }

    public String getClusterIp() {
        return clusterIp;
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

    public long getLastActiveTimeMillis() {
        return lastActiveTimeMillis;
    }

    public void setLastActiveTimeMillis(long lastActiveTimeMillis) {
        this.lastActiveTimeMillis = lastActiveTimeMillis;
    }

    public List<String> getJoinClustTypes() {
        return joinClustTypes;
    }

    public void setJoinClustTypes(List<String> joinClustTypes) {
        this.joinClustTypes = joinClustTypes;
    }

    public void setExtend(Map<String, String> extend) {
        this.extend = extend;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClusterNodeConf that = (ClusterNodeConf) o;
        return Objects.equals(groupId, that.groupId) &&
                Objects.equals(uuid, that.uuid) &&
                Objects.equals(nodeId, that.nodeId) &&
                Objects.equals(nodeName, that.nodeName) &&
                Objects.equals(nodeType, that.nodeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, uuid, nodeId, nodeName, nodeType);
    }

    @Override
    public String toString() {
        return "ClusterNodeConf{" +
                "groupId='" + groupId + '\'' +
                ", uuid='" + uuid + '\'' +
                ", nodeId='" + nodeId + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", nodeType='" + nodeType + '\'' +
                ", joinClustTypes=" + joinClustTypes +
                ", clusterIp='" + clusterIp + '\'' +
                '}';
    }
}
