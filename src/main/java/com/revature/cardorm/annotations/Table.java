package com.revature.cardorm.annotations;


/*
    This annotation exists to allow a table to be specified for postgres manipulation
 */


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Annotation is pointed at classes with .TYPE, is available at runtime, and is documented in javadoc.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented


public @interface Table {
    public String tableName() default "";
}
