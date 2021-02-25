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
package com.inyourcode.dirty;

import com.inyourcode.dirty.api.DirtyAble;
import com.inyourcode.dirty.api.DirtyMetbodAble;

import java.util.HashMap;
import java.util.Map;

/**
 * @author JackLei
 */
public class DirtyAbleMap<K, V>  extends ChildDirtyAble {
    private HashMap<K, V> dataMap = new HashMap<>();

    public DirtyAbleMap(DirtyAble dirtyAble) {
        super(dirtyAble);
    }

    public DirtyAbleMap() {
        super(new ParentDirtyAble());
    }

    @DirtyMetbodAble
    public void put(K k, V v) {
        dataMap.put(k, v);
    }

    @DirtyMetbodAble
    public V delete(K k) {
        return dataMap.remove(k);
    }

    @DirtyMetbodAble
    public void clear() {
        dataMap.clear();
    }

    public int size() {
        return dataMap.size();
    }

    public V get(K k) {
        return dataMap.get(k);
    }

    public Map<K ,V> clone () {
        return new HashMap<>(dataMap);
    }
}
