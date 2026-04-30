package com.ruskserver.deepwither_V2.core.di.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an annotated class is a "Repository" (typically a Data Access Object).
 * Serves as a specialization of @Component, allowing for implementation classes to be autodetected through classpath scanning.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Repository {
}
