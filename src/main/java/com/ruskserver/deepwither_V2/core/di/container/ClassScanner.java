package com.ruskserver.deepwither_V2.core.di.container;

import com.google.common.reflect.ClassPath;
import com.ruskserver.deepwither_V2.core.di.annotations.Ignore;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class ClassScanner {

    /**
     * Scans the specified package recursively and returns a set of classes that are annotated with the given annotation.
     * 
     * @param classLoader The ClassLoader to use. Usually the PluginClassLoader.
     * @param packageName The base package name to scan.
     * @param annotation The annotation to look for.
     * @param logger Optional logger for debug output.
     * @param debugMode If true, logs loading errors.
     * @return A set of classes annotated with the specified annotation.
     */
    public static Set<Class<?>> findClassesWithAnnotation(ClassLoader classLoader, String packageName, Class<? extends Annotation> annotation, Logger logger, boolean debugMode) {
        Set<Class<?>> classes = new HashSet<>();
        try {
            ClassPath classPath = ClassPath.from(classLoader);
            for (ClassPath.ClassInfo classInfo : classPath.getTopLevelClassesRecursive(packageName)) {
                try {
                    // Use Class.forName to avoid initializing the class prematurely
                    Class<?> clazz = Class.forName(classInfo.getName(), false, classLoader);
                    
                    if (clazz.isAnnotationPresent(Ignore.class)) {
                        if (debugMode && logger != null && clazz.isAnnotationPresent(annotation)) {
                            logger.info("[DI-Scanner] Skipped " + clazz.getName() + " (Reason: @Ignore)");
                        }
                        continue;
                    }
                    
                    if (clazz.isAnnotationPresent(annotation)) {
                        classes.add(clazz);
                        if (debugMode && logger != null) {
                            logger.info("[DI-Scanner] Found @" + annotation.getSimpleName() + ": " + clazz.getName());
                        }
                    }
                } catch (NoClassDefFoundError | ClassNotFoundException | ExceptionInInitializerError e) {
                    if (debugMode && logger != null) {
                        logger.warning("[DI-Scanner] Failed to load class " + classInfo.getName() + " - " + e.toString());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }
}
