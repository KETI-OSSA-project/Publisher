package com.example.messagebroker;

/**
 * AIDL interface for messaging service connection state callbacks
 */
interface IMessagingServiceConnectionListener {
    /**
     * Called when the messaging service is connected and ready to use
     */
    void onConnected();

    /**
     * Called when the messaging service is disconnected
     */
    void onDisconnected();
}
