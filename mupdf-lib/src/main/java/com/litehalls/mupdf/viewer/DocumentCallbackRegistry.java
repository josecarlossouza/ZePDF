package com.litehalls.mupdf.viewer;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry to hold DocumentActivityCallback instances.
 * Since callbacks cannot be passed via Intent extras, they must be registered
 * before starting the DocumentActivity and retrieved using a key.
 */
public class DocumentCallbackRegistry {
    private static DocumentCallbackRegistry instance;
    private Map<String, DocumentActivityCallback> callbacks = new HashMap<>();
    
    private DocumentCallbackRegistry() {}
    
    public static synchronized DocumentCallbackRegistry getInstance() {
        if (instance == null) {
            instance = new DocumentCallbackRegistry();
        }
        return instance;
    }
    
    /**
     * Register a callback with the given key.
     * 
     * @param key Unique identifier for this callback
     * @param callback The callback implementation
     */
    public void registerCallback(String key, DocumentActivityCallback callback) {
        callbacks.put(key, callback);
    }
    
    /**
     * Retrieve a callback by key.
     * 
     * @param key The callback identifier
     * @return The callback or null if not found
     */
    public DocumentActivityCallback getCallback(String key) {
        return callbacks.get(key);
    }
    
    /**
     * Unregister a callback.
     * 
     * @param key The callback identifier
     */
    public void unregisterCallback(String key) {
        callbacks.remove(key);
    }
    
    /**
     * Clear all callbacks (useful for cleanup).
     */
    public void clear() {
        callbacks.clear();
    }
}
