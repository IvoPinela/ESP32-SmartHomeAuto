package com.example.smarthomeauto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
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
    private MqttManager mqttManager;
    private View rootView;
    private int userId;
    private String userRole;
    private ImageButton buttonLights;
    private ImageButton buttonGate;
    private ImageButton buttonLogOff;
    private ImageButton buttonAddGuest;
    private ImageButton buttonCreateDevice;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userscreen);

        rootView = findViewById(android.R.id.content);

        // Initialize buttons
        buttonLights = findViewById(R.id.buttonLights);
        buttonGate = findViewById(R.id.buttonGate);
        buttonLogOff = findViewById(R.id.buttonLogOff);
        buttonAddGuest = findViewById(R.id.buttonAddGuest);
        buttonCreateDevice = findViewById(R.id.buttonCreateDevice);

        Intent intent = getIntent();
        userId = intent.getIntExtra("USER_ID", -1);
        userRole = intent.getStringExtra("USER_ROLE");

        // Log user information
        Log.d(TAG, "User ID: " + userId);
        Log.d(TAG, "User Role: " + userRole);

        // Check user role and hide buttonAddGuest if userRole is "guest"
        if ("guest".equals(userRole)) {
            buttonAddGuest.setVisibility(View.GONE);
            buttonCreateDevice.setVisibility(View.GONE);
        }

        // Initialize MqttManager with userId
        mqttManager = new MqttManager(this, userId);

        // Check MQTT permissions
        checkMqttPermissions(userId);

        buttonLights.setOnClickListener(v -> {
            Intent lightsIntent = new Intent(UserActivity.this, LightsControlActivity.class);
            lightsIntent.putExtra("USER_ID", userId);
            lightsIntent.putExtra("USER_ROLE", userRole);
            startActivity(lightsIntent);
        });

        buttonGate.setOnClickListener(v -> {
            Intent gateIntent = new Intent(UserActivity.this, GateControlActivity.class);
            gateIntent.putExtra("USER_ID", userId);
            gateIntent.putExtra("USER_ROLE", userRole);
            startActivity(gateIntent);
        });
        buttonLogOff.setOnClickListener(v -> {
            Intent logoutIntent = new Intent(UserActivity.this, LoginActivity.class);
            startActivity(logoutIntent);
            finish();
        });

        buttonAddGuest.setOnClickListener(v -> {
            Intent newActivityIntent = new Intent(UserActivity.this, GuestListActivity.class);
            newActivityIntent.putExtra("USER_ID", userId);
            newActivityIntent.putExtra("USER_ROLE", userRole);
            startActivity(newActivityIntent);
        });

        buttonCreateDevice.setOnClickListener(v -> {
            Intent createDeviceIntent = new Intent(UserActivity.this, DevicesUserListActivity.class);
            createDeviceIntent.putExtra("USER_ID", userId);
            createDeviceIntent.putExtra("USER_ROLE", userRole);
            startActivity(createDeviceIntent);
        });

    }

    private void checkMqttPermissions(int userId) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            boolean hasNullFields = db.userDao().doesUserHaveNullFields(userId);

            runOnUiThread(() -> {
                if (hasNullFields) {
                    // User is missing MQTT permissions, disable buttons
                    Log.d(TAG, "User is missing MQTT permissions.");
                    showAccessDeniedDialog();
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
        buttonAddGuest.setEnabled(enabled);
        buttonCreateDevice.setEnabled(enabled);
    }

    private void showAccessDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Access Denied")
                .setMessage("You do not have MQTT permissions. Please wait for an admin to grant you access.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttManager != null) {
            mqttManager.disconnect();
        }
    }
}
