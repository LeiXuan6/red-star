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
package com.inyourcode.excel.model;

import com.inyourcode.excel.model.column.CommentColData;
import com.inyourcode.excel.model.column.NameColData;
import com.inyourcode.excel.model.column.RowColData;
import com.inyourcode.excel.model.column.TypeColData;

import java.util.ArrayList;

/**
 * 抽象一个excel sheet的结构
 * @author JackLei
 * @version 2020-01-31
 */
public class SheetDataModel {
    /** sheet名字 */
    private String sheetName;
    /** 第一行为注释 */
    private CommentColData[] commentHeader;
    /** 第二行为数据类型定义 */
    private TypeColData[] typeHeader;
    /** 第三行为名字的定义 */
    private NameColData[] nameHeader;
    /** 行数据 */
    private ArrayList<RowColData[]> rows = new ArrayList<>();

    public SheetDataModel(String sheetName) {
        this.sheetName = sheetName;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public NameColData[] getNameHeader() {
        return nameHeader;
    }

    public void setNameHeader(NameColData[] nameHeader) {
        this.nameHeader = nameHeader;
    }

    public ArrayList<RowColData[]> getRows() {
        return rows;
    }

    public void setRows(ArrayList<RowColData[]> rows) {
        this.rows = rows;
    }

    public CommentColData[] getCommentHeader() {
        return commentHeader;
    }

    public void setCommentHeader(CommentColData[] commentHeader) {
        this.commentHeader = commentHeader;
    }

    public TypeColData[] getTypeHeader() {
        return typeHeader;
    }

    public void setTypeHeader(TypeColData[] typeHeader) {
        this.typeHeader = typeHeader;
    }

    public TypeColData getColTypeData(int col) {
        return  getTypeHeader()[col];
    }
}
