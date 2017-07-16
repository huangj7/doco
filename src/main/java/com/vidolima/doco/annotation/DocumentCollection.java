package com.vidolima.doco.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Place this annotation on fields of an entity POJO. This annotation defines a field of a {@link Document}.
 * 
 * @author James Huang
 * @since July 11, 2017
 */

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DocumentCollection{
    /**
     * Specifies the name of the field.
     * @return name.
     */
    String name() default "";
    
    /**
     * Specifies the CollectionType of the field
     */
    DocumentCollectionType type() default DocumentCollectionType.ARRAYLIST;
}
