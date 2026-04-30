package com.ruskserver.deepwither_V2.core.di.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an annotated class should be ignored by the DI Container's ClassScanner,
 * even if it has @Component, @Service, @Command, etc.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Ignore {
}
