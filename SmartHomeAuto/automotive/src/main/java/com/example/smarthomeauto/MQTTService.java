package com.example.smarthomeauto;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.android.material.snackbar.Snackbar;

public class MQTTService extends Service implements MqttHandler.MessageListener {

    private static final String TAG = "MQTTService";
    private static final String CHANNEL_ID = "mqtt_channel";
    private static final String BROKER_URL = "ssl://05e815044648452d9966e9b6701cb998.s1.eu.hivemq.cloud:8883";
    private static final String USERNAME = "Test1234";
    private static final String PASSWORD = "Test1234";
    private static final String TOPIC = "home/light";

    private MqttHandler mqttHandler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // No binding needed
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        mqttHandler = new MqttHandler(this);
        mqttHandler.connect(BROKER_URL, USERNAME, PASSWORD);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mqttHandler.subscribe(TOPIC);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mqttHandler.disconnect();
    }

    @Override
    public void onMessageReceived(String topic, String message) {
        Log.i(TAG, "Message received on topic " + topic + ": " + message);
        if (TOPIC.equals(topic)) {
            sendNotification("Light Status: " + message);
        }
    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected) {

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "MQTT Channel";
            String description = "Channel for MQTT notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void sendNotification(String message) {
        Intent intent = new Intent(this, UserActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentTitle("MQTT Notification")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
    }
}

