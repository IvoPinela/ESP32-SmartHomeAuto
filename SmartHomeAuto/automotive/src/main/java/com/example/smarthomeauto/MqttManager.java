package com.example.smarthomeauto;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class MqttManager implements MqttHandler.MessageListener {

    private static final String TAG = "MqttManager";
    private static final String BROKER_URL = "ssl://05e815044648452d9966e9b6701cb998.s1.eu.hivemq.cloud:8883";
    private static final String USERNAME = "Test1234";
    private static final String PASSWORD = "Test1234";
    private static final String LIGHT_TOPIC = "home/light";
    private static final String GATE_TOPIC = "home/gate";
    private static final String LIGHT_CHANNEL_ID = "light_status_channel";
    private static final String GATE_CHANNEL_ID = "gate_status_channel";

    private Context context;
    private MqttHandler mqttHandler;
    private boolean isConnected = false;

    public MqttManager(Context context) {
        this.context = context;
        createNotificationChannels();
        requestNotificationPermission();
        mqttHandler = new MqttHandler(this);
        mqttHandler.connect(BROKER_URL, USERNAME, PASSWORD);
    }

    public void connect() {
        mqttHandler.connect(BROKER_URL, USERNAME, PASSWORD);
    }

    public void disconnect() {
        mqttHandler.disconnect();
    }

    public void publishMessage(String topic, String message) {
        Log.i(TAG, "Publishing message to topic " + topic + ": " + message);
        mqttHandler.publish(topic, message);
    }

    public void subscribe() {
        if (isConnected) {
            mqttHandler.subscribe(LIGHT_TOPIC);
            mqttHandler.subscribe(GATE_TOPIC);
        } else {
            Log.e(TAG, "Failed to subscribe: MQTT client is not connected");
        }
    }

    @Override
    public void onMessageReceived(String topic, String message) {
        Log.i(TAG, "Message received on topic " + topic + ": " + message);
        if (LIGHT_TOPIC.equals(topic)) {
            sendNotification(LIGHT_CHANNEL_ID, "Light status updated", "Light is " + message);
        } else if (GATE_TOPIC.equals(topic)) {
            sendNotification(GATE_CHANNEL_ID, "Gate status updated", "Gate is " + message);
        } else if ("MQTT_CONNECTION_STATUS".equals(topic)) {
            isConnected = "Connected".equals(message);
            if (isConnected) {
                subscribe();
            }
        }
    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        this.isConnected = isConnected;
        if (isConnected) {
            subscribe();
        } else {
            Log.e(TAG, "MQTT client is not connected.");
            sendNotification(LIGHT_CHANNEL_ID, "Connection Status", "MQTT client is not connected.");
            sendNotification(GATE_CHANNEL_ID, "Connection Status", "MQTT client is not connected.");
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Light Status Channel
            CharSequence lightName = "Light Status";
            String lightDescription = "Channel for light status notifications";
            int lightImportance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel lightChannel = new NotificationChannel(LIGHT_CHANNEL_ID, lightName, lightImportance);
            lightChannel.setDescription(lightDescription);

            // Gate Status Channel
            CharSequence gateName = "Gate Status";
            String gateDescription = "Channel for gate status notifications";
            int gateImportance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel gateChannel = new NotificationChannel(GATE_CHANNEL_ID, gateName, gateImportance);
            gateChannel.setDescription(gateDescription);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(lightChannel);
                notificationManager.createNotificationChannel(gateChannel);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void sendNotification(String channelId, String title, String message) {
        Log.d(TAG, "Preparing to send notification with message: " + message);

        Intent intent = new Intent(context, LightsControlActivity.class); // Adjust as needed
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(channelId.equals(LIGHT_CHANNEL_ID) ? 1 : 2, builder.build());
            Log.d(TAG, "Notification sent.");
        } else {
            Log.e(TAG, "NotificationManager is null.");
        }
    }
}
