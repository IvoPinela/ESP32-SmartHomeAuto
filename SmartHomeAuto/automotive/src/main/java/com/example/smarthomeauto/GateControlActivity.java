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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class GateControlActivity extends AppCompatActivity implements MqttHandler.MessageListener {

    private static final String TAG = "GateControlActivity";
    private static final String TOPIC = "home/gate";
    private static final String CHANNEL_ID = "gate_status_channel";

    private MqttHandler mqttHandler;
    private TextView gateStatusTextView;
    private boolean isGateOpen = false;
    private boolean isConnected = false;
    private int userId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatescreen);

        Intent intent = getIntent();
        userId = intent.getIntExtra("USER_ID", -1);
        userRole = intent.getStringExtra("USER_ROLE");

        // Initialize MQTT connection
        initializeMqtt();

        // Initialize UI components
        gateStatusTextView = findViewById(R.id.gateStatusTextView);
        Button buttonToggleGate = findViewById(R.id.buttonToggleGate);
        Button buttonBackToMenuGate = findViewById(R.id.buttonBackToMenuGate);

        // Create notification channel
        createNotificationChannel();
        requestNotificationPermission(); // Request notification permission for Android 13+

        // Set click listener for toggle gate button
        buttonToggleGate.setOnClickListener(v -> {
            if (isConnected) {
                // Toggle gate state and publish message
                isGateOpen = !isGateOpen;
                publishMessage(isGateOpen ? "OPEN" : "CLOSED");
                // Update TextView with gate status
                gateStatusTextView.setText("Gate Status: " + (isGateOpen ? "OPEN" : "CLOSED"));
                // Show Snackbar and send notification
                String statusMessage = "Gate is now " + (isGateOpen ? "OPEN" : "CLOSED");
                showSnackbar(statusMessage);
                sendNotification(statusMessage);
            } else {
                Log.e(TAG, "Cannot toggle gate. MQTT client is not connected.");
                String errorMessage = "MQTT client is not connected.";
                showSnackbar(errorMessage);
                sendNotification(errorMessage);
            }
        });

        // Set click listener for back button
        buttonBackToMenuGate.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("USER_ROLE", userRole);
            resultIntent.putExtra("USER_ID", userId);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
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

    private void publishMessage(String message) {
        Log.i(TAG, "Publishing message to topic " + TOPIC + ": " + message);
        mqttHandler.publish(TOPIC, message);
    }

    @Override
    public void onMessageReceived(String topic, String message) {
        Log.i(TAG, "Message received on topic " + topic + ": " + message);
        if (TOPIC.equals(topic)) {
            isGateOpen = "OPEN".equals(message);
            runOnUiThread(() -> {
                String statusMessage = "Gate status updated to " + (isGateOpen ? "OPEN" : "CLOSED");
                gateStatusTextView.setText("Gate Status: " + (isGateOpen ? "OPEN" : "CLOSED"));
                showSnackbar(statusMessage);
                sendNotification(statusMessage);
            });
        }
    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        this.isConnected = isConnected;
        if (isConnected) {
            mqttHandler.subscribe(TOPIC);
        } else {
            Log.e(TAG, "MQTT client is not connected.");
            String errorMessage = "MQTT client is not connected.";
            showSnackbar(errorMessage);
            sendNotification(errorMessage);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Gate Status";
            String description = "Channel for gate status notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.i(TAG, "Notification channel created.");
            } else {
                Log.e(TAG, "Failed to create notification channel.");
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

        Intent intent = new Intent(this, GateControlActivity.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentTitle("Gate Status")
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
