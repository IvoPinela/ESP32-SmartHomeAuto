package com.example.smarthomeauto;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class LightsControlActivity extends AppCompatActivity implements MqttHandler.MessageListener {

    private static final String TAG = "LightsControlActivity";
    private static final String LIGHT_TOPIC = "home/light";
    private static final String GATE_TOPIC = "home/gate";
    private static final String LIGHT_CHANNEL_ID = "light_status_channel";
    private static final String GATE_CHANNEL_ID = "gate_status_channel";

    private MqttHandler mqttHandler;
    private TextView lightStatusTextView;
    private Switch switchLightControl;
    private boolean isLightOn = false;
    private boolean isConnected = false;
    private int userId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lightscreen);

        createNotificationChannels();
        requestNotificationPermission();

        lightStatusTextView = findViewById(R.id.lightStatusTextView);
        switchLightControl = findViewById(R.id.switchLightControl);
        Button buttonBackToMenu = findViewById(R.id.buttonBackToMenu);

        Intent intent = getIntent();
        userId = intent.getIntExtra("USER_ID", -1);
        userRole = intent.getStringExtra("USER_ROLE");

        // Initialize MQTT connection
        initializeMqtt();


        switchLightControl.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isConnected) {
                    isLightOn = isChecked;
                    publishMessage(LIGHT_TOPIC, isLightOn ? "ON" : "OFF");
                    lightStatusTextView.setText("Light Status: " + (isLightOn ? "ON" : "OFF"));
                    sendNotification(LIGHT_CHANNEL_ID, "Light is " + (isLightOn ? "ON" : "OFF"));
                    showSnackbar("Light is " + (isLightOn ? "ON" : "OFF"));
                } else {
                    Log.e(TAG, "Cannot toggle light. MQTT client is not connected.");
                    sendNotification(LIGHT_CHANNEL_ID, "Cannot toggle light. MQTT client is not connected.");
                    showSnackbar("Cannot toggle light. MQTT client is not connected.");
                }
            }
        });

        buttonBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("USER_ROLE", userRole);
                resultIntent.putExtra("USER_ID", userId);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    mqttHandler.subscribe(LIGHT_TOPIC);
                    mqttHandler.subscribe(GATE_TOPIC);
                } else {
                    Log.e(TAG, "Failed to subscribe: MQTT client is not connected");
                }
            }
        }, 2000);
    }

    private void initializeMqtt() {
        new Thread(() -> {
        AppDatabase db = AppDatabase.getDatabase(this);

        // Initialize DAOs
        UserDao userDao = db.userDao();
        BrokerDao brokerDao = db.brokerDao();

        // Fetch brokerID, username, and password
        int brokerId = userDao.getBrokerById(userId);
        String mqttUsername = String.valueOf(userDao.getMqtttUsernameById(userId));
        String mqttPassword = String.valueOf(userDao.getMqtttpassordById(userId));

        // Fetch broker URL and port
        String brokerUrl = brokerDao.getClusterURLById(brokerId);
        int port = brokerDao.getPORTById(brokerId);

        if (brokerUrl == null || port <= 0) {
            Log.e(TAG, "Broker URL or port is invalid.");
            Log.e(TAG, "Broker URL: " + brokerUrl);
            Log.e(TAG, "Port: " + port);
            return;
        }
        String fullBrokerUrl = "ssl://" + brokerUrl + ":" + port;
        mqttHandler = new MqttHandler(this);
        mqttHandler.connect(fullBrokerUrl, mqttUsername, mqttPassword);
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mqttHandler.disconnect();
    }

    private void publishMessage(String topic, String message) {
        Log.i(TAG, "Publishing message to " + topic + ": " + message);
        mqttHandler.publish(topic, message);
    }

    @Override
    public void onMessageReceived(String topic, String message) {
        Log.i(TAG, "Message received on topic " + topic + ": " + message);
        if (LIGHT_TOPIC.equals(topic)) {
            isLightOn = "ON".equals(message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switchLightControl.setChecked(isLightOn);
                    lightStatusTextView.setText("Light Status: " + (isLightOn ? "ON" : "OFF"));
                    sendNotification(LIGHT_CHANNEL_ID, "Light status updated: " + (isLightOn ? "ON" : "OFF"));
                    showSnackbar("Light status updated: " + (isLightOn ? "ON" : "OFF"));
                }
            });
        } else if (GATE_TOPIC.equals(topic)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sendNotification(GATE_CHANNEL_ID, "Gate status: " + message);
                    showSnackbar("Gate status: " + message);
                }
            });
        } else if ("MQTT_CONNECTION_STATUS".equals(topic)) {
            isConnected = "Connected".equals(message);
            if (isConnected) {
                mqttHandler.subscribe(LIGHT_TOPIC);
                mqttHandler.subscribe(GATE_TOPIC);
            } else {
                Log.e(TAG, "MQTT connection lost.");
                sendNotification(LIGHT_CHANNEL_ID, "MQTT connection lost.");
                showSnackbar("MQTT connection lost.");
            }
        }
    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        this.isConnected = isConnected;
        if (isConnected) {
            mqttHandler.subscribe(LIGHT_TOPIC);
            mqttHandler.subscribe(GATE_TOPIC);
        } else {
            Log.e(TAG, "MQTT client is not connected.");
            sendNotification(LIGHT_CHANNEL_ID, "MQTT client is not connected.");
            showSnackbar("MQTT client is not connected.");
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

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(lightChannel);
                notificationManager.createNotificationChannel(gateChannel);
                Log.i(TAG, "Notification channels created.");
            } else {
                Log.e(TAG, "Failed to create notification channels.");
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void sendNotification(String channelId, String message) {
        Log.d(TAG, "Preparing to send notification with message: " + message);

        Intent intent = new Intent(this, LightsControlActivity.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentTitle("Status Update")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            int notificationId = channelId.equals(LIGHT_CHANNEL_ID) ? 1 : 2;
            notificationManager.notify(notificationId, builder.build());
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
