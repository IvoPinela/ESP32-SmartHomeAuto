package com.example.smarthomeauto;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LightsControlActivity extends AppCompatActivity implements MqttHandler.MessageListener {

    private static final String TAG = "LightsControlActivity";
    private static final String LIGHT_TOPIC = "home/light";

    private LinearLayout devicesContainer;
    private MqttHandler mqttHandler;
    private TextView lightStatusTextView;
    private Switch switchLightControl;
    private boolean isLightOn = false;
    private boolean isConnected = false;
    private int userId;
    private String userRole;
    private View rootView;
    private MqttManager mqttManager;
    private final Map<Integer, TextView> deviceStatusTextViewMap = new HashMap<>();
    private final Map<Integer, Switch> deviceSwitchMap = new HashMap<>();
    private boolean isProgrammaticUpdate = false;

    // BroadcastReceiver to handle notifications about light status
    private BroadcastReceiver lightNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            if (message != null) {
                // Process the received message
                Log.i(TAG, "Received light notification: " + message);
                // Update the UI or handle the message as needed
                updateLightStatusFromMessage(message);
                showSnackbar("Light Notification: " + message);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lightscreen);
        rootView = findViewById(android.R.id.content);

        lightStatusTextView = findViewById(R.id.lightStatusTextView);
        switchLightControl = findViewById(R.id.switchLightControl);
        Button buttonBackToMenu = findViewById(R.id.buttonBackToMenu);
        devicesContainer = findViewById(R.id.devicesContainer);

        Intent intent = getIntent();
        userId = intent.getIntExtra("USER_ID", -1);
        userRole = intent.getStringExtra("USER_ROLE");
        mqttManager = new MqttManager(this, userId);
        // Initialize MQTT connection
        initializeMqtt();

        switchLightControl.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isProgrammaticUpdate) {
                    // If the change is programmatic, do not send MQTT message
                    return;
                }

                if (isConnected) {
                    isLightOn = isChecked;
                    publishMessage(LIGHT_TOPIC, isLightOn ? "ON" : "OFF");
                    lightStatusTextView.setText("Light Status: " + (isLightOn ? "ON" : "OFF"));
                    showSnackbar("Light is " + (isLightOn ? "ON" : "OFF"));

                    // Update all device switches
                    for (Map.Entry<Integer, Switch> entry : deviceSwitchMap.entrySet()) {
                        int deviceId = entry.getKey();
                        Switch deviceSwitch = entry.getValue();
                        updateDeviceStatus(deviceId, isChecked);
                    }
                } else {
                    Log.e(TAG, "Cannot toggle light. MQTT client is not connected.");
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

        // Load and display devices
        loadAndDisplayDevices();

        // Register BroadcastReceiver
        IntentFilter filter = new IntentFilter("com.example.smarthomeauto.LIGHT_NOTIFICATION");
        registerReceiver(lightNotificationReceiver, filter);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    mqttHandler.subscribe(LIGHT_TOPIC);
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
        unregisterReceiver(lightNotificationReceiver);
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
            if (LIGHT_TOPIC.equals(topic)) {
                isLightOn = "ON".equals(message);
                setProgrammaticUpdate(true);
                switchLightControl.setChecked(isLightOn);
                setProgrammaticUpdate(false);
                lightStatusTextView.setText("Light Status: " + (isLightOn ? "ON" : "OFF"));
                showSnackbar("Light status updated: " + (isLightOn ? "ON" : "OFF"));
            } else if ("MQTT_CONNECTION_STATUS".equals(topic)) {
                isConnected = "Connected".equals(message);
                if (isConnected) {
                    mqttHandler.subscribe(LIGHT_TOPIC);
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
            mqttHandler.subscribe(LIGHT_TOPIC);
            publishMessage(LIGHT_TOPIC, "Connected");
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

            int lightDeviceTypeId = deviceTypeDao.getIdByName("light");
            List<Device> devices = deviceDao.getDevicesByTypeAndUser(lightDeviceTypeId, userId);

            runOnUiThread(() -> {
                devicesContainer.removeAllViews();
                LayoutInflater inflater = LayoutInflater.from(this);
                for (Device device : devices) {
                    View deviceView = inflater.inflate(R.layout.itemdevicelightgate, devicesContainer, false);

                    TextView deviceNameTextView = deviceView.findViewById(R.id.deviceNameTextView);
                    TextView deviceStatusTextView = deviceView.findViewById(R.id.deviceStatusTextView);
                    Switch deviceSwitch = deviceView.findViewById(R.id.deviceSwitch);

                    deviceNameTextView.setText(device.getName());
                    deviceStatusTextView.setText("Status: OFF");
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
                String message = isOn ? "ON" : "OFF";
                publishMessage(topic, message);

                runOnUiThread(() -> {
                    TextView statusTextView = deviceStatusTextViewMap.get(deviceId);
                    Switch deviceSwitch = deviceSwitchMap.get(deviceId);

                    if (statusTextView != null) {
                        statusTextView.setText("Status: " + (isOn ? "ON" : "OFF"));
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
                        boolean newStatus = "ON".equals(message);
                        statusTextView.setText("Status: " + (newStatus ? "ON" : "OFF"));
                    } else {
                        Log.e(TAG, "No TextView found for device ID: " + deviceId);
                    }

                    if (deviceSwitch != null) {
                        if (isProgrammaticUpdate) {
                            Log.d(TAG, "Skipping programmatic update for device ID: " + deviceId);
                        } else {
                            boolean newStatus = "ON".equals(message);
                            Log.d(TAG, "Updating switch for device ID: " + deviceId + " to " + (newStatus ? "ON" : "OFF"));
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
        switchLightControl.setChecked(allOn);
        lightStatusTextView.setText("Light Status: " + (allOn ? "ON" : "OFF"));
        setProgrammaticUpdate(false);
    }

    private void setProgrammaticUpdate(boolean isProgrammaticUpdate) {
        this.isProgrammaticUpdate = isProgrammaticUpdate;
    }

    private void updateLightStatusFromMessage(String message) {
        // Example message format: "deviceName:ON" or "deviceName:OFF"
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
                        statusTextView.setText("Status: " + (status.equals("ON") ? "ON" : "OFF"));
                    } else {
                        Log.e(TAG, "No TextView found for device ID: " + deviceId);
                    }

                    if (deviceSwitch != null) {
                        if (isProgrammaticUpdate) {
                            Log.d(TAG, "Skipping programmatic update for device ID: " + deviceId);
                        } else {
                            boolean newStatus = status.equals("ON");
                            Log.d(TAG, "Updating switch for device ID: " + deviceId + " to " + (newStatus ? "ON" : "OFF"));
                            deviceSwitch.setChecked(newStatus);
                        }
                    } else {
                        Log.e(TAG, "No Switch found for device ID: " + deviceId);
                    }

                    // Check and update the main light switch status
                    checkAllDeviceStatusAndUpdateMainSwitch();
                });
            } else {
                Log.e(TAG, "No device found for topic: " + topic);
            }
        }).start();
    }

}
