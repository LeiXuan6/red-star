package com.inyourcode.excel.model;

import com.inyourcode.excel.serializer.JavaExcelEnum;
import com.inyourcode.excel.api.ExcelTable;
import com.inyourcode.excel.serializer.JavaListSerializer;
import com.inyourcode.excel.serializer.JavaExcelList;
import com.alibaba.fastjson.annotation.JSONField;
import com.inyourcode.excel.serializer.JavaEnumSerializer;
import com.inyourcode.excel.api.JavaExcelModel;

/***
 * <pre>
 *     此类由工具自动生成的代码.
 *     自定义代码、自定属性、方法可以写在注释内，不会被覆盖.
 * </pre>
 * @author JackLei
 */
@ExcelTable(data = "gift.json")
public class Gift implements JavaExcelModel{

    /** 唯一id */
    private int  id;
    /** 名称 */
    private String  name;
    /** 类型 */
    private int  type;
    /** 是否叠加
["1|Stack|叠加","2|UnStack|非叠加"] */
    @JSONField(deserializeUsing = JavaEnumSerializer.class)
    private GiftEnumAdd  add;
    /** 是否绑定 */
    private int  bind;
    /** 整数数组 */
    @JSONField(deserializeUsing = JavaListSerializer.class)
    private JavaExcelList<Integer>  param1;
    /** 浮点数数组 */
    @JSONField(deserializeUsing = JavaListSerializer.class)
    private JavaExcelList<Integer>  param2;
    /** 参数3字符串数组 */
    @JSONField(deserializeUsing = JavaListSerializer.class)
    private JavaExcelList<String>  param3;

    //自定义逻辑开始(此注释不可修改，不可重复)
    @Override
    public void afterInit() {
    }
    //自定义逻辑结束(此注释不可修改，不可重复)


    public void setId(int id){
        this.id = id;
    }

    public int getId(){
        return this.id;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

    public void setType(int type){
        this.type = type;
    }

    public int getType(){
        return this.type;
    }

    public void setAdd(GiftEnumAdd add){
        this.add = add;
    }

    public GiftEnumAdd getAdd(){
        return this.add;
    }

    public void setBind(int bind){
        this.bind = bind;
    }

    public int getBind(){
        return this.bind;
    }

    public void setParam1(JavaExcelList<Integer> param1){
        this.param1 = param1;
    }

    public JavaExcelList<Integer> getParam1(){
        return this.param1;
    }

    public void setParam2(JavaExcelList<Integer> param2){
        this.param2 = param2;
    }

    public JavaExcelList<Integer> getParam2(){
        return this.param2;
    }

    public void setParam3(JavaExcelList<String> param3){
        this.param3 = param3;
    }

    public JavaExcelList<String> getParam3(){
        return this.param3;
    }

    public static enum GiftEnumAdd implements JavaExcelEnum {
        Stack(1,"Stack"),
        UnStack(2,"UnStack"),

        ;

        private int type;
        private String desc;

        GiftEnumAdd(int type, String desc) {
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

        public  static GiftEnumAdd getEnumType(int type) {
            for(GiftEnumAdd element  : values()) {
                if (element.type == type) {
                    return element;
                }
            }

            return null;
        }
    }


}