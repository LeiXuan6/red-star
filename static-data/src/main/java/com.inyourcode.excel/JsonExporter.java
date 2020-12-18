package com.inyourcode.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.fastjson.JSONObject;
import com.google.common.io.Files;
import com.inyourcode.excel.model.ExcelReaderListener;
import com.inyourcode.excel.model.SheetDataModel;
import com.inyourcode.excel.model.column.CommentColData;
import com.inyourcode.excel.model.column.NameColData;
import com.inyourcode.excel.model.column.RowColData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JsonExporter {
    static Logger LOGGER = LoggerFactory.getLogger(JsonExporter.class);

    protected static void exportJson(String excelPath, String outPath, Map<String, SheetDataModel> sheetDataModelMap) {
        LOGGER.info("JSON_OUT_PATH={}", outPath);
        Map<String, String> jsonMap = convertToJsonFile(sheetDataModelMap);
        doWriteToJsonFile(outPath, jsonMap);
    }

    private static void doWriteToJsonFile(String outPath, Map<String, String> jsonMap) {
        jsonMap.forEach((k, v) -> {
            try {

                File file = new File(outPath + k + ".json");
                if (!file.exists()) {
                    file.createNewFile();
                }

                Files.write(v.getBytes(Charset.forName("UTF-8")), file);
                LOGGER.info("export json:{}", file.getName());
            } catch (IOException e) {
                LOGGER.error("write to json file errpr, out path = {}", outPath, e);
            }
        });
    }

    private static Map<String, String> convertToJsonFile(Map<String, SheetDataModel> sheetDataModelMap) {
        Map<String, String> jsonMap = new HashMap<>();
        sheetDataModelMap.forEach((k, v) -> {
            try {
                CommentColData[] commentHeader = v.getCommentHeader();
                NameColData[] nameHeader = v.getNameHeader();
                ArrayList<RowColData[]> rows = v.getRows();

                if (nameHeader == null) {
                    throw  new RuntimeException("please configure the property columns, sheet name = " + k);
                }

                if (commentHeader == null) {
                    throw  new RuntimeException("please configure the data type columns, sheet name = " + k);
                }

                Map<Object, Map<Object, Object>> rowMap = new HashMap<>();
                for (int index = 0; index < rows.size(); index++) {
                    Map<Object, Object> colMap = new HashMap<>();
                    RowColData[] colDataArray = rows.get(index);
                    Object jsonKey = null;
                    for (int col = 0; col < colDataArray.length; col++) {
                        boolean ignore = false;
                        try {
                            CommentColData c = commentHeader[col];
                            ignore = commentHeader[col].isIgnore();

                            if (!ignore) {
                                if (col == 0) {
                                    jsonKey = colDataArray[col].getVal();
                                }
                                colMap.put(nameHeader[col].getVal(), colDataArray[col].getVal());
                            }
                        } catch (Exception ex) {
                            LOGGER.error(String.format("Error parsing row data ,sheet name = %s, row = %s, col = %s", k, index, col), ex);
                        }
                    }
                    rowMap.put(String.valueOf(jsonKey), colMap);
                }

                String jsonString = JSONObject.toJSONString(rowMap);
                jsonMap.put(k, jsonString);
            } catch (Exception ex) {
                LOGGER.error("JSON Data format conversion failed,sheet name = {} ", k, ex);
                ex.printStackTrace();
                System.exit(-1);
            }
        });

        return jsonMap;
    }

    /**
     * 读取exel数据
     * @param file
     * @param sheetDataModelMap 读取后的excel数据
     */
    public static void scanExcelData(File file, Map<String, SheetDataModel> sheetDataModelMap) {
        if (!file.isDirectory()) {
            if (!file.getName().endsWith("xlsx") && !file.getName().endsWith("xls")) {
                LOGGER.warn("This file [{}] is ignored", file.getName());
            } else {
                ExcelReaderListener excelReaderListener = new ExcelReaderListener();
                ExcelReader excelReader = EasyExcel.read(file, excelReaderListener).doReadAll();

                sheetDataModelMap.putAll(excelReaderListener.getSheetDataModelMap());
                excelReader.finish();
            }
        } else {
            for (File fileTemp : file.listFiles()) {
                scanExcelData(fileTemp, sheetDataModelMap);
            }
        }
    }
}
