/*
 * Copyright (c) 2020 The excel-father Project
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
package com.inyourcode.excel.model.column;

/**
 * 抽象注释列
 * @author JackLei
 * @version 2020-01-31
 */
public class CommentColData extends RowColData {
    /** 这一列在读取时，是否忽略 */
    private boolean ignore;

    public CommentColData(int row, int col, Object val, boolean ignore) {
        super(row, col, val);
        this.ignore = ignore;
    }

    public boolean isIgnore() {
        return ignore;
    }
}
