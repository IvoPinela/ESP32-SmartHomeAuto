package com.example.smarthomeauto;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LightsControlActivity extends AppCompatActivity implements MqttHandler.MessageListener {

    private static final String TAG = "LightsControlActivity";
    private static final String BROKER_URL = "ssl://05e815044648452d9966e9b6701cb998.s1.eu.hivemq.cloud:8883";
    private static final String USERNAME = "Test1234";
    private static final String PASSWORD = "Test1234";
    private static final String TOPIC = "home/light";

    private MqttHandler mqttHandler;
    private TextView lightStatusTextView;
    private boolean isLightOn = false;
    private boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lightscreen);

        mqttHandler = new MqttHandler(this);
        mqttHandler.connect(BROKER_URL, USERNAME, PASSWORD);

        lightStatusTextView = findViewById(R.id.lightStatusTextView);
        Button buttonToggleLight = findViewById(R.id.buttonToggleLight);
        Button buttonBackToMenu = findViewById(R.id.buttonBackToMenu);

        buttonToggleLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected) {
                    isLightOn = !isLightOn;
                    publishMessage(isLightOn ? "ON" : "OFF");
                    lightStatusTextView.setText("Light Status: " + (isLightOn ? "ON" : "OFF"));
                } else {
                    Log.e(TAG, "Cannot toggle light. MQTT client is not connected.");
                }
            }
        });

        buttonBackToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Attempt to subscribe once connected
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
                    lightStatusTextView.setText("Light Status: " + (isLightOn ? "ON" : "OFF"));
                }
            });
        } else if ("MQTT_CONNECTION_STATUS".equals(topic)) {
            isConnected = "Connected".equals(message);
            if (isConnected) {
                // Subscribe to the topic once connected
                mqttHandler.subscribe(TOPIC);
            }
        }
    }

    @Override
    public void onConnectionStatusChanged(boolean isConnected) {
        this.isConnected = isConnected;
        if (isConnected) {
            // Attempt to subscribe when connection is established
            mqttHandler.subscribe(TOPIC);
        } else {
            Log.e(TAG, "MQTT client is not connected.");
        }
    }
}
