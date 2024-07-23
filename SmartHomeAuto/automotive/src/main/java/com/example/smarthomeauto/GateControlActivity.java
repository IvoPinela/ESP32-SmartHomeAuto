package com.example.smarthomeauto;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class GateControlActivity extends AppCompatActivity implements MqttHandler.MessageListener {

    private static final String TAG = "GateControlActivity";
    private static final String BROKER_URL = "ssl://05e815044648452d9966e9b6701cb998.s1.eu.hivemq.cloud:8883";
    private static final String USERNAME = "PublishTest";
    private static final String PASSWORD = "Publish123";
    private static final String TOPIC = "home/gate";

    private MqttHandler mqttHandler;
    private TextView gateStatusTextView;
    private boolean isGateOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatescreen); // Gate Control Layout

        // Initialize MQTT handler with this activity as the listener
        mqttHandler = new MqttHandler(this);

        // Connect to the MQTT broker
        mqttHandler.connect(BROKER_URL, USERNAME, PASSWORD);

        // Subscribe to the topic
        mqttHandler.subscribe(TOPIC);

        // Initialize UI components
        gateStatusTextView = findViewById(R.id.gateStatusTextView);
        Button buttonToggleGate = findViewById(R.id.buttonToggleGate);
        Button buttonBackToMenuGate = findViewById(R.id.buttonBackToMenuGate);

        // Set click listener for toggle gate button
        buttonToggleGate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle gate state and publish message
                isGateOpen = !isGateOpen;
                publishMessage(isGateOpen ? "OPEN" : "CLOSED");
                // Update TextView with gate status
                gateStatusTextView.setText("Gate Status: " + (isGateOpen ? "OPEN" : "CLOSED"));
            }
        });

        // Set click listener for back button
        buttonBackToMenuGate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go back to main menu
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Disconnect MQTT handler
        mqttHandler.disconnect();
    }

    private void publishMessage(String message) {
        Log.i(TAG, "Publishing message: " + message);
        mqttHandler.publish(TOPIC, message);
    }

    @Override
    public void onMessageReceived(String topic, String message) {
        // Handle received message
        Log.i(TAG, "Message received on topic " + topic + ": " + message);
        if (TOPIC.equals(topic)) {
            isGateOpen = "OPEN".equals(message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Update TextView with gate status
                    gateStatusTextView.setText("Gate Status: " + (isGateOpen ? "OPEN" : "CLOSED"));
                }
            });
        }
    }
}
