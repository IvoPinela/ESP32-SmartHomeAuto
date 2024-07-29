package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class UserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userscreen); // Menu Layout

        ImageButton buttonLights = findViewById(R.id.buttonLights);
        ImageButton buttonGate = findViewById(R.id.buttonGate);
        ImageButton buttonLogOff = findViewById(R.id.buttonLogOff);

        buttonLights.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(UserActivity.this, LightsControlActivity.class));
            }
        });

        buttonGate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(UserActivity.this, GateControlActivity.class));
            }
        });

        buttonLogOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Log off and return to login screen
                Intent intent = new Intent(UserActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Close the UserActivity
            }
        });
    }
}