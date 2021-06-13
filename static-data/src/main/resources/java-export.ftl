package ${packageName};

<#list  imports as imp>
import ${imp};
</#list>

/***
 * <pre>
 *     此类由工具自动生成的代码.
 *     自定义代码、自定属性、方法可以写在注释内，不会被覆盖.
 * </pre>
 * @author JackLei
 */
@ExcelTable(data = "${dataFileName}")
public class ${javaClassName} implements JavaExcelModel{

<#list  fields as field>
    /** ${field.comment} */
    <#if field.serializerType = 1>
    @JSONField(deserializeUsing = JavaListSerializer.class)
    </#if>
    <#if field.serializerType = 2>
    @JSONField(deserializeUsing = JavaEnumSerializer.class)
    </#if>
    private ${field.fieldType}  ${field.fieldName};
</#list>

    //自定义逻辑开始(此注释不可修改，不可重复)
${customCode}
    //自定义逻辑结束(此注释不可修改，不可重复)

<#list  fields as field>

    public void set${field.methodNamePrefix}(${field.fieldType} ${field.fieldName}){
        this.${field.fieldName} = ${field.fieldName};
    }

    public ${field.fieldType} get${field.methodNamePrefix}(){
        return this.${field.fieldName};
    }
</#list>

<#list  enumClassList as enumClass>
    public static enum ${enumClass.enumClassName} implements JavaExcelEnum {
        <#list  enumClass.fields as field>
        ${field.desc}(${field.type},"${field.desc}"),
        </#list>

        ;

        private int type;
        private String desc;

        ${enumClass.enumClassName}(int type, String desc) {
            this.type = type;
            this.desc = desc;
        }

        @Override
        public int type() {
            return type;
        }

        @Override
        public String desc() {
            return desc;
        }

        public  static ${enumClass.enumClassName} getEnumType(int type) {
            for(${enumClass.enumClassName} element  : values()) {
                if (element.type == type) {
                    return element;
                }
            }

            return null;
        }
    }
</#list>


}