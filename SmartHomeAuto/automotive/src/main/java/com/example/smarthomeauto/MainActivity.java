package com.example.smarthomeauto;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String BROKER_URL = "ssl://05e815044648452d9966e9b6701cb998.s1.eu.hivemq.cloud:8883";
    private static final String USERNAME = "PublishTest";
    private static final String PASSWORD = "Publish123";

    private static final String TOPIC = "home/light"; // Topic for controlling the light

    private MqttHandler mqttHandler; // Single MQTT handler for publishing
    private TextView lightStatusTextView; // TextView to display the light status
    private boolean isLightOn = false; // Track the light state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize MQTT handler
        mqttHandler = new MqttHandler(null); // No need for a MessageListener in this case

        // Connect to the MQTT broker
        mqttHandler.connect(BROKER_URL, USERNAME, PASSWORD);

        // Find and initialize UI components
        lightStatusTextView = findViewById(R.id.lightStatusTextView);

        // Find the toggle light button
        Button toggleLightButton = findViewById(R.id.btnToggleLight);

        // Set a click listener for the toggle light button
        toggleLightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle the light state
                isLightOn = !isLightOn;
                // Publish the new light state
                publishMessage(isLightOn ? "ON" : "OFF");
                // Update the TextView with the current light status
                lightStatusTextView.setText("Light Status: " + (isLightOn ? "ON" : "OFF"));
            }
        });
    }

    @Override
    protected void onDestroy() {
        // Disconnect MQTT handler
        mqttHandler.disconnect();
        super.onDestroy();
    }

    // Publish a message using the MQTT handler
    private void publishMessage(String message) {
        Log.i(TAG, "Publishing message: " + message);
        mqttHandler.publish(TOPIC, message);
    }
}
