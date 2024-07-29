package com.example.smarthomeauto;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

public class UserActivity extends AppCompatActivity {

    private static final String TAG = "UserActivity";
    private static final String LIGHT_CHANNEL_ID = "light_status_channel";
    private static final String GATE_CHANNEL_ID = "gate_status_channel";
    private MqttManager mqttManager;
    private View rootView;  // Root view to display Snackbar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userscreen);

        rootView = findViewById(android.R.id.content);  // Get the root view for Snackbar

        mqttManager = new MqttManager(this);

        ImageButton buttonLights = findViewById(R.id.buttonLights);
        ImageButton buttonGate = findViewById(R.id.buttonGate);
        ImageButton buttonLogOff = findViewById(R.id.buttonLogOff);

        buttonLights.setOnClickListener(v -> startActivity(new Intent(UserActivity.this, LightsControlActivity.class)));

        buttonGate.setOnClickListener(v -> startActivity(new Intent(UserActivity.this, GateControlActivity.class)));

        buttonLogOff.setOnClickListener(v -> {
            Intent intent = new Intent(UserActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        createNotificationChannels();
        requestNotificationPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mqttManager.disconnect();
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
                Log.i(TAG, "Requested notification permission.");
            }
        }
    }

    public void showSnackbar(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }

    public void updateLightStatus(String status) {
        String message = "Light is " + status;
        showSnackbar(message);
    }

    public void updateGateStatus(String status) {
        String message = "Gate is " + status;
        showSnackbar(message);
    }

    // Call this method to handle MQTT notifications
    public void handleNotification(String channelId, String message) {
        if (LIGHT_CHANNEL_ID.equals(channelId)) {
            updateLightStatus(message);
        } else if (GATE_CHANNEL_ID.equals(channelId)) {
            updateGateStatus(message);
        }
    }
}
