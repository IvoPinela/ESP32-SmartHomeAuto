package com.example.smarthomeauto;

import android.util.Log;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttHandler {

    private MqttClient client;
    private MessageListener messageListener;
    private static final String TAG = "MqttHandler";

    public interface MessageListener {
        void onMessageReceived(String topic, String message);
        void onConnectionStatusChanged(boolean isConnected);
    }

    public MqttHandler(MessageListener listener) {
        this.messageListener = listener;
    }

    public void connect(String brokerUrl, String username, String password) {
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl, MqttClient.generateClientId(), persistence);

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            connectOptions.setUserName(username);
            connectOptions.setPassword(password.toCharArray());

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Connection lost: " + cause.getMessage(), cause);
                    if (messageListener != null) {
                        messageListener.onConnectionStatusChanged(false);
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    if (messageListener != null) {
                        messageListener.onMessageReceived(topic, new String(message.getPayload()));
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            client.connect(connectOptions);
            Log.i(TAG, "Attempting to connect to MQTT broker");
            if (messageListener != null) {
                messageListener.onConnectionStatusChanged(true);
            }
        } catch (MqttException e) {
            Log.e(TAG, "Failed to connect: " + e.getMessage(), e);
            if (messageListener != null) {
                messageListener.onConnectionStatusChanged(false);
            }
        }
    }

    public void disconnect() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                Log.i(TAG, "Disconnected from MQTT broker");
                if (messageListener != null) {
                    messageListener.onConnectionStatusChanged(false);
                }
            } catch (MqttException e) {
                Log.e(TAG, "Failed to disconnect: " + e.getMessage(), e);
            }
        } else {
            Log.e(TAG, "Cannot disconnect. MQTT client is not connected.");
        }
    }

    public void publish(String topic, String message) {
        if (client != null && client.isConnected()) {
            try {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                client.publish(topic, mqttMessage);
                Log.i(TAG, "Published message to topic " + topic + ": " + message);
            } catch (MqttException e) {
                Log.e(TAG, "Failed to publish message: " + e.getMessage(), e);
            }
        } else {
            Log.e(TAG, "Cannot publish message. MQTT client is not connected.");
        }
    }

    public void subscribe(String topic) {
        if (client != null && client.isConnected()) {
            try {
                client.subscribe(topic);
                Log.i(TAG, "Subscribed to topic " + topic);
            } catch (MqttException e) {
                Log.e(TAG, "Failed to subscribe to topic: " + e.getMessage(), e);
            }
        } else {
            Log.e(TAG, "Cannot subscribe to topic. MQTT client is not connected.");
        }
    }
}
