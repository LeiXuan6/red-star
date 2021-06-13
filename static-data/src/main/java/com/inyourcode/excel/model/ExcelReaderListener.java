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

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.read.metadata.holder.ReadRowHolder;
import com.alibaba.excel.read.metadata.holder.ReadSheetHolder;
import com.alibaba.excel.read.metadata.holder.ReadWorkbookHolder;
import com.alibaba.excel.util.CollectionUtils;
import com.alibaba.excel.util.StringUtils;
import com.inyourcode.excel.model.column.CommentColData;
import com.inyourcode.excel.model.column.NameColData;
import com.inyourcode.excel.model.column.RowColData;
import com.inyourcode.excel.model.column.TypeColData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * excel数据读取Listener
 * @author JackLei
 * @version 2020-01-31-17:29
 */
public class ExcelReaderListener extends AnalysisEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelReaderListener.class);
    private static Set<String> sheetNameSet = new HashSet<>();
    private Map<String, SheetDataModel> sheetDataModelMap = new ConcurrentHashMap<>();

    @Override
    public void invoke(Object data, AnalysisContext context) {
        ReadSheetHolder readSheetHolder = context.readSheetHolder();
        String sheetName = readSheetHolder.getSheetName();
        Integer sheetNo = readSheetHolder.getSheetNo();
        //只读第一个sheet
        if (sheetNo >= 1){
            return;
        }
        SheetDataModel sheetDataModel = sheetDataModelMap.get(sheetName);

        ReadWorkbookHolder readWorkbookHolder = context.readWorkbookHolder();
        String fileName = readWorkbookHolder.getFile().getName();

        ReadRowHolder readRowHolder = context.readRowHolder();
        Integer rowIndex = readRowHolder.getRowIndex();

        LinkedHashMap excelRowDataMap = (LinkedHashMap) data;
        if (CollectionUtils.isEmpty(excelRowDataMap)) {
            throw new RuntimeException("There is no data  in this row , file name = " + fileName + ",sheet name = " + sheetName + ",row = " + rowIndex + 1);
        }
        try {
            if (rowIndex == 1) {
                TypeColData[] typeColDataArray = new TypeColData[excelRowDataMap.size()];
                for (int index = 0; index < typeColDataArray.length; index++) {
                    Object val = excelRowDataMap.get(index);

                    TypeColData headerData = new TypeColData(rowIndex, index, val);
                    typeColDataArray[index] = headerData;
                }
                sheetDataModel.setTypeHeader(typeColDataArray);
            } else if (rowIndex == 2) {
                //第3行 字段命名
                NameColData[] nameColDataArray = new NameColData[excelRowDataMap.size()];
                for (int index = 0; index < nameColDataArray.length; index++) {
                    Object val = excelRowDataMap.get(index);

                    NameColData headerData = new NameColData(rowIndex, index, val);
                    nameColDataArray[index] = headerData;
                }
                sheetDataModel.setNameHeader(nameColDataArray);
            } else {
                RowColData[] rowColDatas = new RowColData[excelRowDataMap.size()];
                for (int index = 0; index < rowColDatas.length; index++) {
                    Object val = excelRowDataMap.get(index);
                    if (val == null) {
                        LOGGER.warn("sheetname:{},row[{}],col[{}],no data", sheetName, rowIndex, index);
                    }
                    TypeColData colTypeData = sheetDataModel.getColTypeData(index);
                    RowColData rowData;
                    if (colTypeData.isInt()) {
                        if (val == null) {
                            val = "0";
                        }
                        rowData = new RowColData(rowIndex, index, Integer.valueOf(val.toString()));
                    } else if (colTypeData.isFloat()) {
                        if (val == null) {
                            val = "0";
                        }
                        rowData = new RowColData(rowIndex, index, Float.valueOf(val.toString()));
                    } else if (colTypeData.isString()) {
                        if (val == null) {
                            val = "0";
                        }
                        rowData = new RowColData(rowIndex, index, val.toString());
                    } else {
                        rowData = new RowColData(rowIndex, index, val);
                    }
                    rowColDatas[index] = rowData;
                }
                sheetDataModel.getRows().add(rowColDatas);
            }
        } catch (Exception ex) {
            LOGGER.error(String.format("invoke fail,sheetname=%s,rowIndex=%s", sheetName, rowIndex), ex);
        }

    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        ReadSheetHolder readSheetHolder = context.readSheetHolder();
        String sheetName = readSheetHolder.getSheetName();
        //只读第一个sheet
        Integer sheetNo = readSheetHolder.getSheetNo();
        if (sheetNo >= 1){
            return;
        }

        ReadWorkbookHolder readWorkbookHolder = context.readWorkbookHolder();
        String fileName = readWorkbookHolder.getFile().getName();

        if (sheetNameSet.contains(sheetName)) {
            throw new RuntimeException("Export failed ,duplicate sheet name[" + sheetName + "] ,file = " + fileName);
        }
        sheetNameSet.add(sheetName);
    }

    @Override
    public void invokeHead(Map headMap, AnalysisContext context) {
        super.invokeHead(headMap, context);
        ReadSheetHolder readSheetHolder = context.readSheetHolder();
        Integer sheetNo = readSheetHolder.getSheetNo();
        //只读第一个sheet
        if (sheetNo >= 1){
            return;
        }
        String sheetName = readSheetHolder.getSheetName();

        SheetDataModel sheetDataModel = sheetDataModelMap.get(sheetName);
        if (sheetDataModel == null) {
            sheetDataModel = new SheetDataModel(sheetName);
            SheetDataModel putIfAbsent = sheetDataModelMap.putIfAbsent(sheetName, sheetDataModel);
            if (putIfAbsent != null) {
                sheetDataModel = putIfAbsent;
            }
        }

        CommentColData[] commentColDataArray = new CommentColData[headMap.size()];
        for (int index = 0; index < commentColDataArray.length; index++) {
            Object val = headMap.get(index);

            boolean ignore = false;
            //key值为空时，则忽略
            if(StringUtils.isEmpty(val)){
                ignore = true;
            }

            if(val.toString().contains("#")){
                ignore = true;
            }

            CommentColData headerData = new CommentColData(0,index,val,ignore);
            commentColDataArray[index] = headerData;
        }

        sheetDataModel.setCommentHeader(commentColDataArray);
    }

    public Map<String, SheetDataModel> getSheetDataModelMap() {
        return sheetDataModelMap;
    }
}
