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
package com.inyourcode.excel;

import com.inyourcode.excel.model.SheetDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author JackLei
 * @version 2020-01-31-15:01
 */
public class ExcelFather {
    private static final String EXCEL_ROOT = "/Users/jack/CodeSrcRead/red-star/static-data/src/main/resources/xlsx-conf";
    private static final String JSON_OUT_ROOT = "/Users/jack/CodeSrcRead/red-star/static-data/src/main/resources/server-conf/";
    private static final String JAVA_OUT_ROOT = "/Users/jack/CodeSrcRead/red-star/static-data/src/test/java";
    private static final String TEMPLATE_FILE_ROOT = "/Users/jack/CodeSrcRead/excel-father/src/main/resources";
    private static  final String JAVA_PACKAGE_NAME = "com.inyourcode.excel.model";
    static Logger LOGGER = LoggerFactory.getLogger(ExcelFather.class);

    public static void main(String[] args) {
        String excelPath = EXCEL_ROOT;
        String jsonOutPath = JSON_OUT_ROOT;
        String templatePath = TEMPLATE_FILE_ROOT;
        String javaPackage = JAVA_PACKAGE_NAME;
        String javaOutPath = JAVA_OUT_ROOT;
        LOGGER.info("args.len :{}", args.length);
        if (args.length == 5) {
            excelPath = args[0];
            jsonOutPath = args[1];
            templatePath = args[2];
            javaPackage = args[3];
            javaOutPath = args[4];

            String projectPath = System.getProperty("user.dir");
            int lastIndexOf = projectPath.lastIndexOf(File.separator);
            projectPath = projectPath.substring(0, lastIndexOf);
            excelPath = projectPath + File.separator + excelPath;
            jsonOutPath = projectPath + File.separator + jsonOutPath;
            javaOutPath = projectPath + File.separator + javaOutPath;
            templatePath = projectPath + File.separator + templatePath;
        }

        File file = new File(excelPath);
        LOGGER.info("EXCEL_SRC_PATH = {}", file.getAbsolutePath());
        LOGGER.info("templatePath = {}", templatePath);
        LOGGER.info("javaPackage = {}", javaPackage);
        LOGGER.info("javaOutPath = {}", javaOutPath);

        Map<String, SheetDataModel> sheetDataModelMap = new HashMap<>();
        JsonExporter.scanExcelData(file, sheetDataModelMap);

        JavaExporter javaExporter = new JavaExporter(javaOutPath,"java-export.ftl",templatePath, sheetDataModelMap);
        javaExporter.exportJava(javaPackage);

        JsonExporter.exportJson(excelPath, jsonOutPath, sheetDataModelMap);
    }

}
