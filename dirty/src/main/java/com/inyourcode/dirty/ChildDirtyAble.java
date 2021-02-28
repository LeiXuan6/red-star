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

/**
 * @author JackLei
 */
public class ChildDirtyAble implements DirtyAble {
    protected transient DirtyMonitor monitor;

    public ChildDirtyAble(DirtyMonitor dirtyAble) {
        this.monitor = dirtyAble;
    }

    @Override
    public void markDirty() {
        monitor.markDirty();
    }

    @Override
    public void unMarkDirty() {
        monitor.unMarkDirty();
    }

    @Override
    public boolean isDirty() {
        return monitor.isDirty();
    }
}
