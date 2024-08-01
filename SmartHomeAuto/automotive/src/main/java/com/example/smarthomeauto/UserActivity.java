package com.example.smarthomeauto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class UserActivity extends AppCompatActivity {

    private static final String TAG = "UserActivity";
    private static final String LIGHT_CHANNEL_ID = "light_status_channel";
    private static final String GATE_CHANNEL_ID = "gate_status_channel";
    private MqttManager mqttManager;
    private View rootView;
    private int userId;
    private String userRole;
    private ImageButton buttonLights;
    private ImageButton buttonGate;
    private ImageButton buttonLogOff;
    private ImageButton buttonAddGuest; /// Novo botão

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userscreen);

        rootView = findViewById(android.R.id.content);

        mqttManager = new MqttManager(this);

        Intent intent = getIntent();
        userId = intent.getIntExtra("USER_ID", -1);
        userRole = intent.getStringExtra("USER_ROLE");

        // Log user information
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "User Role: " + userRole);

        // Initialize buttons
        buttonLights = findViewById(R.id.buttonLights);
        buttonGate = findViewById(R.id.buttonGate);
        buttonLogOff = findViewById(R.id.buttonLogOff);
        buttonAddGuest= findViewById(R.id.buttonAddGuest);

        // Check MQTT permissions
        checkMqttPermissions(userId);

        buttonLights.setOnClickListener(v -> startActivity(new Intent(UserActivity.this, LightsControlActivity.class)));
        buttonGate.setOnClickListener(v -> startActivity(new Intent(UserActivity.this, GateControlActivity.class)));
        buttonLogOff.setOnClickListener(v -> {
            Intent logoutIntent = new Intent(UserActivity.this, LoginActivity.class);
            startActivity(logoutIntent);
            finish();
        });

        buttonAddGuest.setOnClickListener(v -> {
            Intent newActivityIntent = new Intent(UserActivity.this, GuestListActivity.class);
            newActivityIntent.putExtra("USER_ID", userId);
            startActivity(newActivityIntent);
        });

        createNotificationChannels();
        requestNotificationPermission();
    }

    private void checkMqttPermissions(int userId) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            boolean hasNullFields = db.userDao().doesUserHaveNullFields(userId);

            runOnUiThread(() -> {
                if (hasNullFields) {
                    // User is missing MQTT permissions, disable buttons
                    Log.d(TAG, "User is missing MQTT permissions.");
                    showAccessDeniedMessage();
                    enableButtons(false);
                } else {
                    // User has valid MQTT permissions, enable buttons
                    Log.d(TAG, "User has valid MQTT permissions.");
                    enableButtons(true);
                }
            });
        }).start();
    }

    private void enableButtons(boolean enabled) {
        buttonLights.setEnabled(enabled);
        buttonGate.setEnabled(enabled);
    }

    private void showAccessDeniedMessage() {
        String message = "Access denied. Please wait for an admin to grant you permissions.";
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
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
