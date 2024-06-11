package com.codeages.termiusplus.biz.server.annotation;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.*;

/**
 * @author kuozhi
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SftpActive {

     /**
      * 列名
      */
     @Language("SpEL")
     String value();
}
