// IMessagingServiceConnectionListener.aidl
package com.example.messagebroker;

// Declare any non-default types here with import statements

/**
 * Callback interface for MessagingService connection state changes.
 * Used to notify clients when the service is connected or disconnected.
 */
interface IMessagingServiceConnectionListener {
    /**
     * Called when the MessagingService is successfully connected and ready to use.
     */
    void onConnected();

    /**
     * Called when the MessagingService is disconnected.
     * This can happen due to service crashes, unbinding, or other connection issues.
     */
    void onDisconnected();
}