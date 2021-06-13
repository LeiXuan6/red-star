package com.inyourcode.excel;

import com.google.common.io.Files;
import com.inyourcode.excel.model.JavaExportClass;
import com.inyourcode.excel.model.SheetDataModel;
import com.inyourcode.excel.model.column.CommentColData;
import com.inyourcode.excel.model.column.NameColData;
import com.inyourcode.excel.model.column.TypeColData;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JavaExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaExporter.class);
    private static final Set<String> IMPORTS = new HashSet<>(Arrays.asList( "com.inyourcode.excel.serializer.JavaExcelList",
                                                                            "com.inyourcode.excel.serializer.JavaExcelEnum",
                                                                            "com.inyourcode.excel.api.ExcelTable",
                                                                            "com.inyourcode.excel.api.JavaExcelModel",
                                                                            "com.inyourcode.excel.serializer.JavaEnumSerializer",
                                                                            "com.inyourcode.excel.serializer.JavaListSerializer",
                                                                            "com.alibaba.fastjson.annotation.JSONField"));
    private static final String DEFAULT_CONSTOM_CODE = "    @Override\n" +
                                                        "    public void afterInit() {\n" +
                                                        "    }";
    private Template template;
    private String rootPath;
    private String templateName;
    private String templatePath;
    private Map<String, SheetDataModel> sheetDataModelMap;

    public JavaExporter(String rootPath, String templateName, String templatePath, Map<String, SheetDataModel> sheetDataModelMap) {
        this.rootPath = rootPath;
        this.templateName = templateName;
        this.templatePath = templatePath;
        this.sheetDataModelMap = sheetDataModelMap;
        init();
    }

    public void init() {
        Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setDefaultEncoding("UTF-8");
        try {
            configuration.setDirectoryForTemplateLoading(new File(templatePath));
            template = configuration.getTemplate(templateName,"UTF-8");
        } catch (IOException e) {
            LOGGER.error("init template configuration error", e);
        }

    }

    public void exportJava(String packageName){
        sheetDataModelMap.forEach((name, model)->{
            String javaClassName = name.substring(0,1).toUpperCase() + name.substring(1);
            JavaExportClass javaExportClass = new JavaExportClass(packageName, javaClassName);
            javaExportClass.setImports(IMPORTS);
            javaExportClass.setDataFileName(name + ".json");
            javaExportClass.setCustomCode(DEFAULT_CONSTOM_CODE);
            CommentColData[] commentHeader = model.getCommentHeader();
            NameColData[] nameHeader = model.getNameHeader();
            TypeColData[] typeHeader = model.getTypeHeader();
            try {
                for (int index = 0; index < nameHeader.length; index++) {
                    String type = "";
                    int serializeType = 0;
                    if (typeHeader[index].isFloat()) {
                        type = "float";
                    } else if (typeHeader[index].isInt()) {
                        type = "int";
                    } else if (typeHeader[index].isString()) {
                        type = "String";
                    } else if (typeHeader[index].isIntArry()) {
                        type = "JavaExcelList<Integer>";
                        serializeType = 1;
                    } else if (typeHeader[index].isFloatArray()) {
                        type = "JavaExcelList<Integer>";
                        serializeType = 1;
                    } else if (typeHeader[index].isStringArray()) {
                        type = "JavaExcelList<String>";
                        serializeType = 1;
                    } else if (typeHeader[index].isEnum()) {
                        serializeType = 2;
                        String enumHeaderStr = nameHeader[index].getVal().toString();
                        type = javaClassName + "Enum" + enumHeaderStr.substring(0, 1).toUpperCase() + enumHeaderStr.substring(1);

                        JavaExportClass.JavaExportEnum javaExportEnumClazz = new JavaExportClass.JavaExportEnum();
                        javaExportEnumClazz.setEnumClassName(type);

                        String enumCommentStr = commentHeader[index].getVal().toString();
                        try {
                            int startIndex = enumCommentStr.indexOf("[") + 1;
                            int endIndex = enumCommentStr.indexOf("]");
                            String enumStr = enumCommentStr.substring(startIndex, endIndex).replaceAll("\n", "");
                            String[] enumStrArray = enumStr.split("\\|");

                            for (String enumElement : enumStrArray) {
                                String[] enumFieldEntry = enumElement.split(":");

                                JavaExportClass.JavaExportEnumElement enumElementClazz = new JavaExportClass.JavaExportEnumElement();
                                enumElementClazz.setType(Integer.valueOf(enumFieldEntry[0]));
                                enumElementClazz.setDesc(enumFieldEntry[1]);
                                javaExportEnumClazz.getFields().add(enumElementClazz);
                            }
                            javaExportClass.getEnumClassList().add(javaExportEnumClazz);
                        } catch (Exception ex) {
                            LOGGER.error("export java enum error, This format configuration is incorrect，name:{}, enum:{}", name, enumCommentStr);
                            continue;
                        }

                    } else {
                        LOGGER.error("export java error, type not found, type:{}, data:{},name:{}", typeHeader[index].getVal(), typeHeader[index], name);
                        continue;
                    }

                    JavaExportClass.JavaExportField javaExportField = new JavaExportClass.JavaExportField(type, nameHeader[index].getVal().toString(), commentHeader[index].getVal().toString(), serializeType);
                    javaExportClass.getFields().add(javaExportField);
                }
            }catch (Exception ex) {
                LOGGER.error("export java error, type not found, sheentName:{}, export java failed", name);
            }
            genJavaClass(javaExportClass);
        });
    }

    private void genJavaClass(JavaExportClass javaExportInfo) {
        try {
            String packageName = javaExportInfo.getPackageName();
            String javaClassName = javaExportInfo.getJavaClassName() + ".java";
            String out = rootPath.concat(Stream.of(packageName.split("\\."))
                                 .collect(Collectors.joining("/", "/", "/" + javaClassName)));
            LOGGER.info("export java class,path = {}", out);
            File fileOut = new File(out);
            if (!fileOut.exists()) {
                fileOut.createNewFile();
            } else {
                List<String> fileContentList = Files.readLines(fileOut, Charset.forName("UTF-8"));
                int startIndex = 0;
                int endIndex = 0;
                for (int index = 0; index < fileContentList.size(); index++) {
                    String content = fileContentList.get(index);
                    if (content.contains("自定义逻辑开始")) {
                        startIndex = index;
                    }

                    if (content.contains("自定义逻辑结束")) {
                        endIndex = index;
                        break;
                    }
                }

                if (startIndex > 0 && endIndex > 0) {
                    String code = "";
                    for (int index = startIndex + 1 ; index < endIndex; index++) {
                        code += fileContentList.get(index);
                        if (index < endIndex - 1) {
                            code += "\r\n";
                        }
                    }
                    javaExportInfo.setCustomCode(code);
                }
            }
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(fileOut));
            template.process(javaExportInfo, outputStreamWriter);
        } catch (IOException | TemplateException e) {
            LOGGER.error("expor java class error", e);
        }
    }
}
