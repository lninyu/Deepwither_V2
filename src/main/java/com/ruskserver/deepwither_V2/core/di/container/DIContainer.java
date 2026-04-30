package com.ruskserver.deepwither_V2.core.di.container;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Module;
import com.ruskserver.deepwither_V2.core.di.annotations.Repository;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.di.exceptions.CircularDependencyException;
import com.ruskserver.deepwither_V2.core.di.exceptions.DependencyResolutionException;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Logger;

public class DIContainer {
    
    private final Map<Class<?>, Object> instances = new HashMap<>();
    private final LinkedHashSet<Class<?>> resolvingClasses = new LinkedHashSet<>();
    private final Map<Class<?>, List<Class<?>>> dependencyGraph = new HashMap<>();
    
    private Logger logger;
    private boolean debugMode = false;

    public DIContainer() {
        // Register the container itself so it can be injected if needed
        registerInstance(DIContainer.class, this);
    }
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Manually registers a specific instance for a class type.
     */
    public <T> void registerInstance(Class<T> type, T instance) {
        instances.put(type, instance);
        if (!dependencyGraph.containsKey(type)) {
            dependencyGraph.put(type, new ArrayList<>());
        }
    }

    /**
     * Gets or creates an instance of the requested type.
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type) {
        if (instances.containsKey(type)) {
            return (T) instances.get(type);
        }

        // Circular dependency check
        if (resolvingClasses.contains(type)) {
            List<String> path = new ArrayList<>();
            for (Class<?> clazz : resolvingClasses) {
                path.add(clazz.getSimpleName());
            }
            path.add(type.getSimpleName());
            String trace = String.join(" -> ", path);
            throw new CircularDependencyException("Circular dependency detected! Resolution path: " + trace);
        }

        if (debugMode && logger != null) {
            logger.info("[DI-Container] Resolving dependencies for: " + type.getName());
        }

        resolvingClasses.add(type);

        try {
            T instance = createInstance(type);
            instances.put(type, instance);
            
            if (debugMode && logger != null) {
                logger.info("[DI-Container] Instantiated: " + type.getName());
            }
            
            return instance;
        } finally {
            resolvingClasses.remove(type);
        }
    }

    /**
     * Creates an instance by looking for an @Inject constructor or a default constructor.
     */
    @SuppressWarnings("unchecked")
    private <T> T createInstance(Class<T> type) {
        if (type.isInterface()) {
            throw new DependencyResolutionException("Cannot instantiate an interface: " + type.getName() + ". Please bind an implementation or use concrete classes.");
        }

        Constructor<?>[] constructors = type.getDeclaredConstructors();
        Constructor<?> targetConstructor = null;

        // Look for @Inject
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Inject.class)) {
                targetConstructor = constructor;
                break;
            }
        }

        // If no @Inject, fallback to default constructor (no-args)
        if (targetConstructor == null) {
            try {
                targetConstructor = type.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new DependencyResolutionException("No @Inject constructor found and no default constructor found for: " + type.getName());
            }
        }

        targetConstructor.setAccessible(true);
        Class<?>[] parameterTypes = targetConstructor.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];
        
        List<Class<?>> dependencies = new ArrayList<>();

        for (int i = 0; i < parameterTypes.length; i++) {
            dependencies.add(parameterTypes[i]);
            parameters[i] = resolve(parameterTypes[i]); // Recursively resolve dependencies
        }
        
        dependencyGraph.put(type, dependencies);

        try {
            return (T) targetConstructor.newInstance(parameters);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new DependencyResolutionException("Failed to instantiate class: " + type.getName(), e);
        }
    }

    /**
     * Scans a package and automatically resolves and registers all annotated classes
     */
    public void scanAndRegister(ClassLoader classLoader, String basePackage) {
        Set<Class<?>> targetClasses = new HashSet<>();
        targetClasses.addAll(ClassScanner.findClassesWithAnnotation(classLoader, basePackage, Component.class, logger, debugMode));
        targetClasses.addAll(ClassScanner.findClassesWithAnnotation(classLoader, basePackage, Service.class, logger, debugMode));
        targetClasses.addAll(ClassScanner.findClassesWithAnnotation(classLoader, basePackage, Repository.class, logger, debugMode));
        targetClasses.addAll(ClassScanner.findClassesWithAnnotation(classLoader, basePackage, Module.class, logger, debugMode));
        targetClasses.addAll(ClassScanner.findClassesWithAnnotation(classLoader, basePackage, Command.class, logger, debugMode));

        for (Class<?> clazz : targetClasses) {
            // Force resolution and instantiation of scanned classes
            resolve(clazz);
        }
    }

    /**
     * Calls start() on all registered instances that implement Startable.
     */
    public void startAll() {
        for (Object instance : instances.values()) {
            if (instance instanceof Startable) {
                ((Startable) instance).start();
            }
        }
    }

    /**
     * Calls stop() on all registered instances that implement Stoppable.
     */
    public void stopAll() {
        for (Object instance : instances.values()) {
            if (instance instanceof Stoppable) {
                try {
                    ((Stoppable) instance).stop();
                } catch (Exception e) {
                    if (logger != null) {
                        logger.warning("Error stopping component " + instance.getClass().getName() + ": " + e.getMessage());
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    /**
     * Returns a collection of all registered instances.
     */
    public Collection<Object> getAllInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }
    
    /**
     * Prints the entire dependency tree to the configured logger.
     */
    public void printDependencyTree() {
        if (logger == null) return;
        
        logger.info("=== [DI Tree] Registered Components ===");
        
        // Find root nodes (classes that no one depends on)
        Set<Class<?>> allDeps = new HashSet<>();
        for (List<Class<?>> deps : dependencyGraph.values()) {
            allDeps.addAll(deps);
        }
        
        Set<Class<?>> roots = new HashSet<>(dependencyGraph.keySet());
        roots.removeAll(allDeps);
        
        // Sort roots alphabetically
        List<Class<?>> sortedRoots = new ArrayList<>(roots);
        sortedRoots.sort(Comparator.comparing(Class::getName));
        
        for (Class<?> root : sortedRoots) {
            printNode(root, "", true, new HashSet<>());
        }
        logger.info("=======================================");
    }
    
    private void printNode(Class<?> node, String prefix, boolean isTail, Set<Class<?>> visited) {
        if (logger == null) return;
        
        String nodeName = node.getSimpleName();
        // Prevent infinite printing loop in case of unhandled circular refs (though should be blocked earlier)
        if (visited.contains(node)) {
            logger.info(prefix + (isTail ? "└── " : "├── ") + nodeName + " (Circular Reference)");
            return;
        }
        visited.add(node);
        
        logger.info(prefix + (isTail ? "└── " : "├── ") + nodeName);
        
        List<Class<?>> deps = dependencyGraph.getOrDefault(node, new ArrayList<>());
        for (int i = 0; i < deps.size() - 1; i++) {
            printNode(deps.get(i), prefix + (isTail ? "    " : "│   "), false, new HashSet<>(visited));
        }
        if (deps.size() > 0) {
            printNode(deps.get(deps.size() - 1), prefix + (isTail ? "    " : "│   "), true, new HashSet<>(visited));
        }
    }
}
