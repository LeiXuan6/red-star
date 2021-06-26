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

import com.inyourcode.cluster.api.IClusterNodeType;

/**
 * @author JackLei
 */
public enum ClusterType implements IClusterNodeType {
    LOBBY(1,"lobby"),
    MATCH(2,"match"),
    CHAT(3,"chat"),
    WEB(4,"web"),
    LOGIN(5,"login"),
    FIGHT(6,"fight"),
    COMMON(7,"chat"),
    MAIL(8,"mail"),
    FRIEND(9,"friend")




    ;


    private int type;
    private String name;

    ClusterType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public static ClusterType getType(String name) {
        for (ClusterType clusterType : values()) {
            if (clusterType.name.equals(name)) {
                return  clusterType;
            }
        }

        return null;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }
}
