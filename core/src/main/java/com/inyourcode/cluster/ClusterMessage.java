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

import java.util.HashSet;
import java.util.Set;

/**
 * @author JackLei
 */
public class ClusterMessage {
    private byte serializerCode;
    private byte messageCode;
    private String sourceNodeId;
    private Set<String> targetNodeIds = new HashSet<>();
    private Object data;

    public String getSourceNodeId() {
        return sourceNodeId;
    }

    public void setSourceNodeId(String sourceNodeId) {
        this.sourceNodeId = sourceNodeId;
    }

    public Set<String> getTargetNodeIds() {
        return targetNodeIds;
    }

    public void setTargetNodeIds(Set<String> targetNodeIds) {
        this.targetNodeIds = targetNodeIds;
    }

    public void addClusterNode(String clusterNodeId){
        this.targetNodeIds.add(clusterNodeId);
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public byte getSerializerCode() {
        return serializerCode;
    }

    public void setSerializerCode(byte serializerCode) {
        this.serializerCode = serializerCode;
    }

    public byte getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(byte messageCode) {
        this.messageCode = messageCode;
    }

    @Override
    public String toString() {
        return "ClusterMessage{" +
                "serializerCode=" + serializerCode +
                ", messageCode=" + messageCode +
                ", clusterNodeIds=" + targetNodeIds +
                ", data=" + data +
                '}';
    }
}
