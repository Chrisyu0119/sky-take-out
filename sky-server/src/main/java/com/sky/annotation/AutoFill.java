package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定義註解，用於標示某個方法需要進行功能字段自動填充處裡
 */
@Target(ElementType.METHOD)//指定這個註解只能加在方法上
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    //指定我們當前這個數據庫的操作類型
    //數據庫操作類型:UPDATE,INSERT
    OperationType value();
}
