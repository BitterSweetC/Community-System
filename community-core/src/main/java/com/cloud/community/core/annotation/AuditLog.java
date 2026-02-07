package com.cloud.community.core.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    
    /**
     * Action name (e.g. LOGIN, CREATE, UPDATE)
     */
    String action();

    /**
     * Resource type (e.g. CLUB, USER)
     */
    String resourceType() default "";

    /**
     * SpEL expression to get resource ID
     */
    String resourceId() default "";

    /**
     * SpEL expression to get detail info
     */
    String detail() default "";

    /**
     * SpEL expression to get user ID (if not from SecurityContext)
     */
    String userId() default "";
}
