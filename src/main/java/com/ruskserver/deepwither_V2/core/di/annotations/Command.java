package com.ruskserver.deepwither_V2.core.di.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an annotated class is a Command and should be automatically
 * registered with the Paper Command API (LifecycleEventManager).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {
    /**
     * The name of the command.
     */
    String name();

    /**
     * Optional description of the command.
     */
    String description() default "";

    /**
     * Optional aliases for the command.
     */
    String[] aliases() default {};
}
