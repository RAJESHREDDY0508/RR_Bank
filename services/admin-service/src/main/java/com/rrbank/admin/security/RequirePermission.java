package com.rrbank.admin.security;

import com.rrbank.admin.entity.Permission;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to require specific permissions for accessing endpoints
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    
    /**
     * Required permissions - user must have ALL of these
     */
    Permission[] value() default {};
    
    /**
     * Alternative: user must have ANY of these permissions
     */
    Permission[] anyOf() default {};
    
    /**
     * If true, having any one permission is sufficient
     */
    boolean requireAll() default true;
}
