package com.example.smarthomeauto;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttHandler {

    private MqttClient client; // MQTT client instance
    private MessageListener messageListener; // Listener for received messages
    private static final String TAG = "MqttHandler";


    // Interface for handling received messages
    public interface MessageListener {
        void onMessageReceived(String topic, String message);
    }

    // Constructor to set the message listener
    public MqttHandler(MessageListener listener) {
        this.messageListener = listener;
    }

    // Connect to the MQTT broker with the given credentials
    public void connect(String brokerUrl, String username, String password) {
        try {
            MemoryPersistence persistence = new MemoryPersistence(); // Memory persistence for the client
            client = new MqttClient(brokerUrl, MqttClient.generateClientId(), persistence);

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true); // Set a clean session
            connectOptions.setUserName(username); // Set the username
            connectOptions.setPassword(password.toCharArray()); // Set the password

            client.connect(connectOptions); // Connect to the broker

            // Set the callback for the client
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Connection lost: " + cause.getMessage(), cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // Notify the listener when a message arrives
                    if (messageListener != null) {
                        messageListener.onMessageReceived(topic, new String(message.getPayload()));
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Handle delivery complete
                }
            });

        } catch (MqttException e) {
            Log.e(TAG, "Failed to connect: " + e.getMessage(), e);
        }
    }

    // Disconnect from the MQTT broker
    public void disconnect() {
        try {
            client.disconnect(); // Disconnect the client
        } catch (MqttException e) {
            Log.e(TAG, "Failed to disconnect: " + e.getMessage(), e);
        }
    }

    // Publish a message to the specified topic
    public void publish(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes()); // Create an MQTT message
            client.publish(topic, mqttMessage); // Publish the message
        } catch (MqttException e) {
            Log.e(TAG, "Failed to publish message: " + e.getMessage(), e);
        }
    }

    // Subscribe to the specified topic
    public void subscribe(String topic) {
        try {
            client.subscribe(topic); // Subscribe to the topic
        } catch (MqttException e) {
            Log.e(TAG, "Failed to subscribe to topic: " + e.getMessage(), e);
        }
    }
}
