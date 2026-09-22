package com.campus.annotation;

import java.lang.annotation.*;

/**
 * 操作审计日志注解
 * 标注在 Controller 方法上，AOP 自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    /** 操作模块 */
    String module() default "";
    /** 操作类型（如：创建、更新、删除） */
    String operation() default "";
}
