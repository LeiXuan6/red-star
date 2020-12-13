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
 * 抽象excel的一列数据
 * @author JackLei
 * @version 2020-01-31
 */
public class RowColData<T> {
    /** 行号 从0开始 */
    private int row ;
    /** 列号 从0开始 */
    private int col ;
    /** 值 */
    private T val;

    public RowColData(int row, int col, T val) {
        this.row = row;
        this.col = col;
        this.val = val;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public T getVal() {
        return val;
    }

    @Override
    public String toString() {
        return "RowColData{" +
                "row=" + row +
                ", col=" + col +
                ", val=" + val +
                '}';
    }
}
