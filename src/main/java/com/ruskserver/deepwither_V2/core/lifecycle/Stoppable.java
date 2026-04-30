package com.ruskserver.deepwither_V2.core.lifecycle;

/**
 * Interface for components that require termination logic
 * when the plugin is disabled.
 */
public interface Stoppable {
    void stop();
}
