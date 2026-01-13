package com.cwq.project_aicodingstation.common.annotation;

import java.lang.annotation.*;

/**
 * 只要给方法添加了 @AuthCheck 注解，就必须要登录，否则会抛出异常。
 * E.g., `@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)`
 * */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthCheck {

    /**
     * 必须具备的角色
     * 例如：ADMIN/USER
     */
    String mustRole() default "";
}

/*
 * interface:   This keyword is used to declare a traditional interface in Java.
 *              An interface in Java is an abstract type that is used to specify a behavior
 *              that classes must implement. It contains static constants and abstract methods.
 * @interface:  This keyword is used to declare a new annotation type. Annotations can be used
 *              to provide metadata for your Java code. For example, the @Override annotation
 *              indicates that a method is intended to override a method in a superclass.
 * */