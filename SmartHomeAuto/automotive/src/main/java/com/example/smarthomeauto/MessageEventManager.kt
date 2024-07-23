package com.example.smarthomeauto

// Class responsible for managing listeners and notifying them of received messages
class MessageEventManager {
    // List to hold registered message listeners
    private val listeners: MutableList<MqttHandler.MessageListener> = ArrayList()

    // Method to add a new listener
    fun addListener(listener: MqttHandler.MessageListener) {
        listeners.add(listener)
    }

    // Method to remove an existing listener
    fun removeListener(listener: MqttHandler.MessageListener) {
        listeners.remove(listener)
    }

    // Method to notify all registered listeners about a received message
    fun notifyMessageReceived(topic: String?, message: String?) {
        for (listener in listeners) {
            listener.onMessageReceived(topic, message)
        }
    }
}
