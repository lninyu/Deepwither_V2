package com.ruskserver.deepwither_V2.core.di.exceptions;

public class CircularDependencyException extends RuntimeException {
    public CircularDependencyException(String message) {
        super(message);
    }
}
