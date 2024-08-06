package com.example.smarthomeauto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GateControlActivity extends AppCompatActivity implements MqttHandler.MessageListener {

    private static final String TAG = "GateControlActivity";
    private static final String TOPIC = "home/gate";
    private LinearLayout devicesContainer;
    private MqttHandler mqttHandler;
    private TextView GateStatusTextView;
    private Switch switchGateControl;
    private boolean isGateOn = false;
    private boolean isConnected = false;
    private int userId;
    private String userRole;
    private View rootView;
    private MqttManager mqttManager;
    private final Map<Integer, TextView> deviceStatusTextViewMap = new HashMap<>();
    private final Map<Integer, Switch> deviceSwitchMap = new HashMap<>();
    private boolean isProgrammaticUpdate = false;

    // BroadcastReceiver to handle notifications about gate status
    private BroadcastReceiver GateNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            if (message != null) {
                // Process the received message
                Log.i(TAG, "Received Gate notification: " + message);
                // Update the UI or handle the message as needed
                updateGateStatusFromMessage(message);
                showSnackbar("Gate Notification: " + message);
            }
        }
    };

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatescreen);
        rootView = findViewById(android.R.id.content);

        GateStatusTextView = findViewById(R.id.gateStatusTextView);
        switchGateControl = findViewById(R.id.switchGateControl);
        Button buttonBackToMenu = findViewById(R.id.buttonBackToMenuGate);
        devicesContainer = findViewById(R.id.devicesContainer);

        Intent intent = getIntent();
        userId = intent.getIntExtra("USER_ID", -1);
        userRole = intent.getStringExtra("USER_ROLE");
        mqttManager = new MqttManager(this, userId);
        // Initialize MQTT connection
        initializeMqtt();

        switchGateControl.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isProgrammaticUpdate) {
                    // If the change is programmatic, do not send MQTT message
                    return;
                }

                if (isConnected) {
                    isGateOn = isChecked;
                    publishMessage(TOPIC, isGateOn ? "OPEN" : "CLOSE");
                    GateStatusTextView.setText("Gate Status: " + (isGateOn ? "OPEN" : "CLOSE"));
                    showSnackbar("Gate is " + (isGateOn ? "OPEN" : "CLOSE"));

                    // Update all device switches
                    for (Map.Entry<Integer, Switch> entry : deviceSwitchMap.entrySet()) {
                        int deviceId = entry.getKey();
                        Switch deviceSwitch = entry.getValue();
                        updateDeviceStatus(deviceId, isChecked);
                    }
                } else {
                    Log.e(TAG, "Cannot toggle Gate. MQTT client is not connected.");
                    showSnackbar("Cannot toggle Gate. MQTT client is not connected.");
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

        // Load and display devices
        loadAndDisplayDevices();

        // Register BroadcastReceiver
        IntentFilter filter = new IntentFilter("com.example.smarthomeauto.GATE_NOTIFICATION");
        registerReceiver(GateNotificationReceiver, filter);

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
            mqttHandler = new MqttHandler(this); // Pass this instance
            mqttHandler.connect(fullBrokerUrl, mqttUsername, mqttPassword);
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mqttHandler.disconnect();
        // Unregister BroadcastReceiver
        unregisterReceiver(GateNotificationReceiver);
    }

    private void publishMessage(String topic, String message) {
        Log.i(TAG, "Publishing message to " + topic + ": " + message);
        mqttHandler.publish(topic, message);
    }

    @Override
    public void onMessageReceived(String topic, String message) {
        Log.i(TAG, "Message received on topic " + topic + ": " + message);
        Log.d(TAG, "Processing message for topic: " + topic);

        runOnUiThread(() -> {
            if (TOPIC.equals(topic)) {
                isGateOn = "OPEN".equals(message);
                setProgrammaticUpdate(true);
                switchGateControl.setChecked(isGateOn);
                setProgrammaticUpdate(false);
                GateStatusTextView.setText("Gate Status: " + (isGateOn ? "OPEN" : "CLOSE"));
                showSnackbar("Gate status updated: " + (isGateOn ? "OPEN" : "CLOSE"));
            } else if ("MQTT_CONNECTION_STATUS".equals(topic)) {
                isConnected = "Connected".equals(message);
                if (isConnected) {
                    mqttHandler.subscribe(TOPIC);
                } else {
                    Log.e(TAG, "MQTT connection lost.");
                    showSnackbar("MQTT connection lost.");
                }
            } else {
                // Handle unrecognized topics
                Log.d(TAG, "Handling unrecognized topic: " + topic);
                handleUnrecognizedMessage(topic, message);
            }
        });
    }

    private void handleUnrecognizedMessage(String topic, String message) {
        // Optional: Add logic to process or log unrecognized messages
        Log.d(TAG, "Received unrecognized message on topic: " + topic + " with message: " + message);
    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        this.isConnected = isConnected;
        if (isConnected) {
            mqttHandler.subscribe(TOPIC);
            publishMessage(TOPIC, "Connected");
        } else {
            Log.e(TAG, "MQTT client is not connected.");
            showSnackbar("MQTT client is not connected.");
        }
    }

    private void showSnackbar(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }

    private void loadAndDisplayDevices() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            DeviceDao deviceDao = db.deviceDao();
            DeviceTypeDao deviceTypeDao = db.deviceTypeDao();

            int gateDeviceTypeId = deviceTypeDao.getIdByName("gate");
            List<Device> devices = deviceDao.getDevicesByTypeAndUser(gateDeviceTypeId, userId);

            runOnUiThread(() -> {
                devicesContainer.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(this);
                for (Device device : devices) {
                    View deviceView = inflater.inflate(R.layout.itemdevicelightgate, devicesContainer, false);

                    TextView deviceNameTextView = deviceView.findViewById(R.id.deviceNameTextView);
                    TextView deviceStatusTextView = deviceView.findViewById(R.id.deviceStatusTextView);
                    Switch deviceSwitch = deviceView.findViewById(R.id.deviceSwitch);

                    deviceNameTextView.setText(device.getName());
                    deviceStatusTextView.setText("Status: CLOSE");
                    deviceSwitch.setChecked(false);

                    deviceStatusTextViewMap.put(device.getId(), deviceStatusTextView);
                    deviceSwitchMap.put(device.getId(), deviceSwitch);

                    deviceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        updateDeviceStatus(device.getId(), isChecked);
                        checkAllDeviceStatusAndUpdateMainSwitch();
                    });

                    devicesContainer.addView(deviceView);
                }
            });
        }).start();
    }

    private void updateDeviceStatus(int deviceId, boolean isOn) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            DeviceDao deviceDao = db.deviceDao();

            String topic = deviceDao.getTopicById(deviceId);
            if (topic != null) {
                String message = isOn ? "OPEN" : "CLOSE";
                publishMessage(topic, message);

                runOnUiThread(() -> {
                    TextView statusTextView = deviceStatusTextViewMap.get(deviceId);
                    Switch deviceSwitch = deviceSwitchMap.get(deviceId);

                    if (statusTextView != null) {
                        statusTextView.setText("Status: " + (isOn ? "OPEN" : "CLOSE"));
                    } else {
                        Log.e(TAG, "No TextView found for device ID: " + deviceId);
                    }

                    if (deviceSwitch != null) {
                        setProgrammaticUpdate(true);
                        deviceSwitch.setChecked(isOn);
                        setProgrammaticUpdate(false);
                    } else {
                        Log.e(TAG, "No Switch found for device ID: " + deviceId);
                    }
                });
            } else {
                Log.e(TAG, "No topic found for device ID: " + deviceId);
            }
        }).start();
    }

    private void updateDeviceStatusFromMessage(String message) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            DeviceDao deviceDao = db.deviceDao();

            String[] topicParts = message.split("/");
            if (topicParts.length < 3) {
                Log.e(TAG, "Topic format is incorrect: " + message);
                return;
            }

            String deviceName = topicParts[2];
            Integer deviceId = deviceDao.getDeviceIdByName(deviceName);

            if (deviceId != null) {
                runOnUiThread(() -> {
                    TextView statusTextView = deviceStatusTextViewMap.get(deviceId);
                    Switch deviceSwitch = deviceSwitchMap.get(deviceId);

                    if (statusTextView != null) {
                        boolean newStatus = "OPEN".equals(message);
                        statusTextView.setText("Status: " + (newStatus ? "OPEN" : "CLOSE"));
                    } else {
                        Log.e(TAG, "No TextView found for device ID: " + deviceId);
                    }

                    if (deviceSwitch != null) {
                        if (isProgrammaticUpdate) {
                            Log.d(TAG, "Skipping programmatic update for device ID: " + deviceId);
                        } else {
                            boolean newStatus = "ON".equals(message);
                            Log.d(TAG, "Updating switch for device ID: " + deviceId + " to " + (newStatus ? "OPEN" : "CLOSE"));
                            deviceSwitch.setChecked(newStatus);
                        }
                    } else {
                        Log.e(TAG, "No Switch found for device ID: " + deviceId);
                    }
                    checkAllDeviceStatusAndUpdateMainSwitch();
                });
            } else {
                Log.e(TAG, "No device found for name: " + deviceName);
            }
        }).start();
    }

    private void checkAllDeviceStatusAndUpdateMainSwitch() {
        boolean allOn = true;
        for (Switch deviceSwitch : deviceSwitchMap.values()) {
            if (!deviceSwitch.isChecked()) {
                allOn = false;
                break;
            }
        }

        setProgrammaticUpdate(true);
        switchGateControl.setChecked(allOn);
        GateStatusTextView.setText("Gate Status: " + (allOn ? "OPEN" : "CLOSE"));
        setProgrammaticUpdate(false);
    }

    private void setProgrammaticUpdate(boolean isProgrammaticUpdate) {
        this.isProgrammaticUpdate = isProgrammaticUpdate;
    }

    private void updateGateStatusFromMessage(String message) {
        String[] parts = message.split(":");
        if (parts.length != 2) {
            Log.e(TAG, "Invalid message format: " + message);
            return;
        }

        String topic = parts[0];
        String status = parts[1];

        // Update device status based on received message
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            DeviceDao deviceDao = db.deviceDao();
            Integer deviceId = deviceDao.getDeviceIdByTopic(topic);

            if (deviceId != null) {
                runOnUiThread(() -> {
                    TextView statusTextView = deviceStatusTextViewMap.get(deviceId);
                    Switch deviceSwitch = deviceSwitchMap.get(deviceId);

                    if (statusTextView != null) {
                        statusTextView.setText("Status: " + (status.equals("OPEN") ? "OPEN" : "CLOSE"));
                    } else {
                        Log.e(TAG, "No TextView found for device ID: " + deviceId);
                    }

                    if (deviceSwitch != null) {
                        if (isProgrammaticUpdate) {
                            Log.d(TAG, "Skipping programmatic update for device ID: " + deviceId);
                        } else {
                            boolean newStatus = status.equals("OPEN");
                            Log.d(TAG, "Updating switch for device ID: " + deviceId + " to " + (newStatus ? "OPEN" : "CLOSE"));
                            deviceSwitch.setChecked(newStatus);
                        }
                    } else {
                        Log.e(TAG, "No Switch found for device ID: " + deviceId);
                    }

                    // Check and update the main gate switch status
                    checkAllDeviceStatusAndUpdateMainSwitch();
                });
            } else {
                Log.e(TAG, "No device found for topic: " + topic);
            }
        }).start();
    }

}