package com.inyourcode.excel.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class JavaExportClass {
    private String packageName;
    private String javaClassName;
    private String dataFileName;
    private String customCode;
    private Set<JavaExportField> fields = new LinkedHashSet<>();
    private Set<String> imports = new LinkedHashSet<>();
    private List<JavaExportEnum> enumClassList = new ArrayList<>();

    public JavaExportClass(String packageName, String javaClassName) {
        this.packageName = packageName;
        this.javaClassName = javaClassName.substring(0 ,1).toUpperCase() + javaClassName.substring(1);
    }

    public String getPackageName() {
        return packageName;
    }

    public String getJavaClassName() {
        return javaClassName;
    }

    public Set<String> getImports() {
        return imports;
    }

    public void setImports(Set<String> imports) {
        this.imports = imports;
    }

    public Set<JavaExportField> getFields() {
        return fields;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setJavaClassName(String javaClassName) {
        this.javaClassName = javaClassName;
    }

    public void setFields(Set<JavaExportField> fields) {
        this.fields = fields;
    }

    public String getDataFileName() {
        return dataFileName;
    }

    public void setDataFileName(String dataFileName) {
        this.dataFileName = dataFileName;
    }

    public String getCustomCode() {
        return customCode;
    }

    public void setCustomCode(String customCode) {
        this.customCode = customCode;
    }

    public List<JavaExportEnum> getEnumClassList() {
        return enumClassList;
    }

    public void setEnumClassList(List<JavaExportEnum> enumClassList) {
        this.enumClassList = enumClassList;
    }

    public static class JavaExportField {
        private String fieldType;
        private String fieldName;
        private String methodNamePrefix;
        private String comment;
        /* json 序列化类型**/
        private int serializerType;

        public JavaExportField(String fieldType, String fieldName, String comment,int serializerType) {
            this.fieldType = fieldType;
            this.fieldName = fieldName;
            this.comment = comment;
            this.methodNamePrefix = this.fieldName.substring(0, 1).toUpperCase() + this.fieldName.substring(1);
            this.serializerType = serializerType;
        }

        public String getFieldType() {
            return fieldType;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getMethodNamePrefix() {
            return methodNamePrefix;
        }

        public int getSerializerType() {
            return serializerType;
        }

        public void setSerializerType(int serializerType) {
            this.serializerType = serializerType;
        }

        public void setFieldType(String fieldType) {
            this.fieldType = fieldType;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public void setMethodNamePrefix(String methodNamePrefix) {
            this.methodNamePrefix = methodNamePrefix;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JavaExportField that = (JavaExportField) o;
            return fieldType.equals(that.fieldType) &&
                    fieldName.equals(that.fieldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fieldType, fieldName);
        }
    }

    public static class JavaExportEnum {
        private String enumClassName;
        private List<JavaExportEnumElement> fields = new ArrayList<>();

        public String getEnumClassName() {
            return enumClassName;
        }

        public void setEnumClassName(String enumClassName) {
            this.enumClassName = enumClassName;
        }

        public List<JavaExportEnumElement> getFields() {
            return fields;
        }

        public void setFields(List<JavaExportEnumElement> fields) {
            this.fields = fields;
        }
    }

    public static class JavaExportEnumElement {
        private int type;
        private String desc;

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }

}





