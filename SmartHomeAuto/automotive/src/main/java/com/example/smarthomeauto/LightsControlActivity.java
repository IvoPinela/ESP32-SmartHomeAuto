package com.example.smarthomeauto;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationCompat;
import com.google.android.material.snackbar.Snackbar; // Adicione essa importação
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class LightsControlActivity extends AppCompatActivity implements MqttHandler.MessageListener {

    private static final String TAG = "LightsControlActivity";
    private static final String BROKER_URL = "ssl://05e815044648452d9966e9b6701cb998.s1.eu.hivemq.cloud:8883";
    private static final String USERNAME = "Test1234";
    private static final String PASSWORD = "Test1234";
    private static final String TOPIC = "home/light";
    private static final String CHANNEL_ID = "light_status_channel";

    private MqttHandler mqttHandler;
    private TextView lightStatusTextView;
    private Switch switchLightControl;
    private boolean isLightOn = false;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lightscreen);

        createNotificationChannel();
        requestNotificationPermission(); // Solicitar permissão para Android 13+

        mqttHandler = new MqttHandler(this);
        mqttHandler.connect(BROKER_URL, USERNAME, PASSWORD);

        lightStatusTextView = findViewById(R.id.lightStatusTextView);
        switchLightControl = findViewById(R.id.switchLightControl);
        Button buttonBackToMenu = findViewById(R.id.buttonBackToMenu);

        switchLightControl.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isConnected) {
                    isLightOn = isChecked;
                    publishMessage(isLightOn ? "ON" : "OFF");
                    lightStatusTextView.setText("Light Status: " + (isLightOn ? "ON" : "OFF"));
                    sendNotification("Light is " + (isLightOn ? "ON" : "OFF"));
                    showSnackbar("Light is " + (isLightOn ? "ON" : "OFF")); // Mostrar Snackbar
                } else {
                    Log.e(TAG, "Cannot toggle light. MQTT client is not connected.");
                    sendNotification("MQTT client is not connected.");
                    showSnackbar("MQTT client is not connected."); // Mostrar Snackbar
                }
            }
        });

        buttonBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    mqttHandler.subscribe(TOPIC);
                } else {
                    Log.e(TAG, "Failed to subscribe: MQTT client is not connected");
                }
            }
        }, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mqttHandler.disconnect();
    }

    private void publishMessage(String message) {
        Log.i(TAG, "Publishing message: " + message);
        mqttHandler.publish(TOPIC, message);
    }

    @Override
    public void onMessageReceived(String topic, String message) {
        Log.i(TAG, "Message received on topic " + topic + ": " + message);
        if (TOPIC.equals(topic)) {
            isLightOn = "ON".equals(message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switchLightControl.setChecked(isLightOn);
                    lightStatusTextView.setText("Light Status: " + (isLightOn ? "ON" : "OFF"));
                    sendNotification("Light is " + (isLightOn ? "ON" : "OFF"));
                    showSnackbar("Light is " + (isLightOn ? "ON" : "OFF")); // Mostrar Snackbar
                }
            });
        } else if ("MQTT_CONNECTION_STATUS".equals(topic)) {
            isConnected = "Connected".equals(message);
            if (isConnected) {
                mqttHandler.subscribe(TOPIC);
            }
        }
    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        this.isConnected = isConnected;
        if (isConnected) {
            mqttHandler.subscribe(TOPIC);
        } else {
            Log.e(TAG, "MQTT client is not connected.");
            sendNotification("MQTT client is not connected.");
            showSnackbar("MQTT client is not connected."); // Mostrar Snackbar
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Light Status";
            String description = "Channel for light status notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void sendNotification(String message) {
        Log.d(TAG, "Preparing to send notification with message: " + message);

        Intent intent = new Intent(this, LightsControlActivity.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentTitle("Light Status")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
            Log.d(TAG, "Notification sent.");
        } else {
            Log.e(TAG, "NotificationManager is null.");
        }
    }

    private void showSnackbar(String message) {
        View rootView = findViewById(android.R.id.content);
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }
}
