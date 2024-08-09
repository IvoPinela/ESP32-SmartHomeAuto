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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MqttManager implements MqttHandler.MessageListener {

    private static final String TAG = "MqttManager";
    private static final String LIGHT_CHANNEL_ID = "light_notifications";
    private static final String GATE_CHANNEL_ID = "gate_notifications";
    private List<String> subscribedTopics = new ArrayList<>();
    private Map<String, Integer> notificationIdMap = new HashMap<>();
    private AtomicInteger notificationIdCounter = new AtomicInteger(0);

    private Context context;
    private MqttHandler mqttHandler;
    private boolean isConnected = false;
    private int UserId;
    private String USERNAME;
    private String PASSWORD;
    private String BROKER_URL;

    public MqttManager(Context context, int userid) {
        this.context = context;
        createNotificationChannels();
        requestNotificationPermission();
        mqttHandler = new MqttHandler(this);
        UserId = userid;
        iniciateMqtt();
    }

    private void iniciateMqtt() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);

            // Initialize DAOs
            UserDao userDao = db.userDao();
            BrokerDao brokerDao = db.brokerDao();
            DeviceDao deviceDao = db.deviceDao();

            Log.e(TAG, "ID " + UserId);

            // Fetch brokerID, username, and password
            int brokerId = userDao.getBrokerById(UserId);
            USERNAME = String.valueOf(userDao.getMqtttUsernameById(UserId));
            PASSWORD = String.valueOf(userDao.getMqtttpassordById(UserId));

            // Fetch broker URL and port
            String brokerUrl = brokerDao.getClusterURLById(brokerId);
            int port = brokerDao.getPORTById(brokerId);

            if (brokerUrl == null || port <= 0) {
                Log.e(TAG, "Broker URL or port is invalid.");
                Log.e(TAG, "Broker URL " + brokerUrl);
                Log.e(TAG, "Broker URL " + port);
                return;
            }

            // Update BROKER_URL with the full URL
            BROKER_URL = "ssl://" + brokerUrl + ":" + port;

            // Connect to the broker after fetching the details
            connect();
        }).start();
    }

    public void connect() {
        mqttHandler.connect(BROKER_URL, USERNAME, PASSWORD);
    }

    public void disconnect() {
        mqttHandler.disconnect();
    }

    private void sendBroadcastForLightTopic(String message) {
        Intent intent = new Intent("com.example.smarthomeauto.LIGHT_NOTIFICATION");
        intent.putExtra("message", message);
        context.sendBroadcast(intent);
    }
    private void sendBroadcastForGateTopic(String message) {
        Intent intent = new Intent("com.example.smarthomeauto.GATE_NOTIFICATION");
        intent.putExtra("message", message);
        context.sendBroadcast(intent);
    }

    public void subscribe() {
        if (isConnected) {
            AppDatabase db = AppDatabase.getDatabase(context);
            DeviceDao deviceDao = db.deviceDao();
            UserDao userDao = db.userDao();
            DeviceTypeDao deviceTypeDao = db.deviceTypeDao();
            UserDeviceDao userDeviceDao = db.userDeviceDao();

            String userRole = userDao.getRoleById(UserId);
            if (userRole.equals("user")) {
                List<Device> devices = deviceDao.getDevicesByUserId(UserId);
                for (Device device : devices) {
                    String deviceTopic = device.getMqttSubTopic();
                    mqttHandler.subscribe(deviceTopic);
                    String principalTopic = deviceTypeDao.getMqttPrincipalTopicById(device.TypeId);
                    subscribedTopics.add(deviceTopic);
                    if (principalTopic != null) {
                        if (!subscribedTopics.contains(principalTopic)) {
                            mqttHandler.subscribe(principalTopic);
                            subscribedTopics.add(principalTopic);
                        }
                    }
                }
            }  else if ("guest".equals(userRole)) {
                // Guest user can only subscribe to devices they have access to via UserDevice
                List<Integer> deviceIds = userDeviceDao.getReadableDeviceIdsByUserId(UserId);
                Log.d(TAG, "Device IDs retrieved for guest: " + deviceIds.size());
                for (int deviceId : deviceIds) {
                    Device device = deviceDao.getDeviceById(deviceId);
                    if (device != null) {
                        String deviceTopic = device.getMqttSubTopic();
                        mqttHandler.subscribe(deviceTopic);
                        subscribedTopics.add(deviceTopic);
                        Log.d(TAG, "Subscribing to device topic: " + deviceTopic);
                    }

                }
            }
        } else {
            Log.e(TAG, "Failed to subscribe: MQTT client is not connected");
        }
    }
    @Override
    public void onMessageReceived(String topic, String message) {
        Log.i(TAG, "Message received on topic " + topic + ": " + message);
        String channelId = determineChannelId(topic);
        int notificationId = getNotificationIdForTopic(topic); // Generate a unique notification ID based on the topic
        String formattedMessage = formatMessage(topic, message);
        String title = generateNotificationTitle(topic); // Generate a more specific title based on the topic
        sendNotification(channelId, notificationId, title, formattedMessage);

        if (topic.startsWith("home/light/")) {
            sendBroadcastForLightTopic(topic+":"+message);
        }
        if (topic.startsWith("home/gate/")) {
            sendBroadcastForGateTopic(topic+":"+message);
        }

    }

    private String generateNotificationTitle(String topic) {

        if (topic.startsWith("home/light")) {
            return "Light Status Update";
        } else if (topic.startsWith("home/gate")) {
            return "Gate Status Update";
        } else {
            return "Status Update";
        }
    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        this.isConnected = isConnected;
        if (isConnected) {
            subscribe();
        } else {
            Log.e(TAG, "MQTT client is not connected.");
            int notificationId = notificationIdCounter.incrementAndGet(); // Generate a unique notification ID
            sendNotification(LIGHT_CHANNEL_ID, notificationId, "Connection Status", "MQTT client is not connected.");
            sendNotification(GATE_CHANNEL_ID, notificationId, "Connection Status", "MQTT client is not connected.");
        }
    }

    private String determineChannelId(String topic) {
        // Define the logic to determine the channel ID based on the topic
        if (topic.startsWith("home/light/")) {
            return LIGHT_CHANNEL_ID;
        } else if (topic.startsWith("home/gate/")) {
            return GATE_CHANNEL_ID;
        } else {
            // Fallback channel or default behavior
            return LIGHT_CHANNEL_ID; // or return a default channel ID
        }
    }

    private int getNotificationIdForTopic(String topic) {
        // Use a map to keep track of notification IDs for each topic
        return notificationIdMap.computeIfAbsent(topic, k -> notificationIdCounter.incrementAndGet());
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

            if (notificationManager == null) {
                Log.e(TAG, "NotificationManager is null.");
                return;
            }

            // Light Notifications Channel
            CharSequence lightName = "Light Notifications";
            String lightDescription = "Notifications for light status updates";
            int lightImportance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel lightChannel = new NotificationChannel(LIGHT_CHANNEL_ID, lightName, lightImportance);
            lightChannel.setDescription(lightDescription);

            // Gate Notifications Channel
            CharSequence gateName = "Gate Notifications";
            String gateDescription = "Notifications for gate status updates";
            int gateImportance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel gateChannel = new NotificationChannel(GATE_CHANNEL_ID, gateName, gateImportance);
            gateChannel.setDescription(gateDescription);

            // Create channels if they don't already exist
            boolean lightChannelExists = false;
            boolean gateChannelExists = false;

            for (NotificationChannel channel : notificationManager.getNotificationChannels()) {
                if (LIGHT_CHANNEL_ID.equals(channel.getId())) {
                    lightChannelExists = true;
                }
                if (GATE_CHANNEL_ID.equals(channel.getId())) {
                    gateChannelExists = true;
                }
            }

            if (!lightChannelExists) {
                notificationManager.createNotificationChannel(lightChannel);
            }
            if (!gateChannelExists) {
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

    private void sendNotification(String channelId, int notificationId, String title, String message) {
        Log.d(TAG, "Preparing to send notification with message: " + message);

        Intent intent = new Intent(context, LightsControlActivity.class);
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
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification sent with ID: " + notificationId);
        } else {
            Log.e(TAG, "NotificationManager is null.");
        }
    }

    private String formatMessage(String topic, String message) {
        String[] parts = topic.split("/");

        // Handle cases where there are exactly 2 parts
        if (parts.length == 2) {
            String category = parts[1];
            // Use category from split parts to format the message
            if (category.equals("light")) {
                // Check if message is "ON" or "OFF" (case insensitive)
                if (message.equalsIgnoreCase("ON")) {
                    return "All lights: turned on";
                } else if (message.equalsIgnoreCase("OFF")) {
                    return "All lights: turned off";
                } else {
                    Log.w(TAG, "Unrecognized light status message: " + message);
                    return "Light status unknown: " + message;
                }
            } else if (category.equals("gate")) {
                // Check if message is "OPEN" or "CLOSED" (case insensitive)
                if (message.equalsIgnoreCase("OPEN")) {
                    return "All gates: opened";
                } else if (message.equalsIgnoreCase("CLOSE")) {
                    return "All gates: closed";
                } else {
                    Log.w(TAG, "Unrecognized gate status message: " + message);
                    return "Gate status unknown: " + message;
                }
            } else {
                Log.w(TAG, "Unrecognized category in topic: " + category);
                return "Unrecognized category: " + category;
            }
        }

        // Handle cases where there are exactly 3 parts (for more specific topics)
        if (parts.length == 3) {
            String type = parts[1];
            String location = parts[2];

            String formattedType = type.equals("light") ? "Light" : type;
            if(type=="gate") {
                formattedType = type.equals("gate") ? "gate" : type;
            }
            String formattedMessage;

            if (message.equalsIgnoreCase("ON") || message.equalsIgnoreCase("OPEN")) {
                formattedMessage = "turned on";
            } else if (message.equalsIgnoreCase("OFF") || message.equalsIgnoreCase("CLOSE")) {
                formattedMessage = "turned off";
            } else {
                Log.w(TAG, "Unrecognized message for specific topic: " + message);
                formattedMessage = "unknown status: " + message;
            }

            return String.format("%s %s: %s", formattedType, location, formattedMessage);
        }

        // Handle cases with unrecognized topic structure
        Log.w(TAG, "Unrecognized topic structure: " + topic);
        return "Unrecognized topic: " + topic;
    }

}
