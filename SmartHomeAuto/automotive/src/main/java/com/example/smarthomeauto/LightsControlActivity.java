package com.example.smarthomeauto;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LightsControlActivity extends AppCompatActivity {

    private static final String TAG = "LightsControlActivity";
    private static final String BROKER_URL = "ssl://05e815044648452d9966e9b6701cb998.s1.eu.hivemq.cloud:8883";
    private static final String USERNAME = "PublishTest";
    private static final String PASSWORD = "Publish123";
    private static final String TOPIC = "home/light";

    private MqttHandler mqttHandler;
    private TextView lightStatusTextView;
    private boolean isLightOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lightscreen); // Lights Control Layout

        // Initialize MQTT handler
        mqttHandler = new MqttHandler(null);

        // Connect to the MQTT broker
        mqttHandler.connect(BROKER_URL, USERNAME, PASSWORD);

        // Initialize UI components
        lightStatusTextView = findViewById(R.id.lightStatusTextView);
        Button buttonToggleLight = findViewById(R.id.buttonToggleLight);
        Button buttonBackToMenu = findViewById(R.id.buttonBackToMenu);

        // Set click listener for toggle light button
        buttonToggleLight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle light state and publish message
                isLightOn = !isLightOn;
                publishMessage(isLightOn ? "ON" : "OFF");
                // Update TextView with light status
                lightStatusTextView.setText("Light Status: " + (isLightOn ? "ON" : "OFF"));
            }
        });

        // Set click listener for back button
        buttonBackToMenu.setOnClickListener(new View.OnClickListener() {
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
}
