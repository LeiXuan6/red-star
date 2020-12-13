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
 * 抽象数据类型列
 * @author JackLei
 * @version 2020-01-31
 */
public class TypeColData extends RowColData {

    public TypeColData(int row, int col, Object val) {
        super(row, col, val);
    }

    public boolean isInt() {
        return getVal().equals("int");
    }

    public boolean isFloat() {
        return getVal().equals("float");
    }

    public  boolean isString() {
        return getVal().equals("string");
    }

    public boolean isIntArry() {
        return getVal().equals("int[]");
    }

    public boolean isFloatArray() {
        return getVal().equals("float[]");
    }

    public boolean isStringArray(){
        return getVal().equals("string[]");
    }

    public boolean isEnum() {
        return getVal().equals("enum");
    }
}
