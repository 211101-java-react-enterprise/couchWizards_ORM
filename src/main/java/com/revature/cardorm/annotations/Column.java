package com.revature.cardorm.annotations;

/*
    The following annotation allows for a column to be set to interact with abstractly
 */

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented

public @interface Column {
    public String columnName() default "";
}
