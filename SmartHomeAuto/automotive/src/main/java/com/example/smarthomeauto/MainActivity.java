package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.initialscreen); // Menu Layout

        Button buttonLights = findViewById(R.id.buttonLights);
        Button buttonGate = findViewById(R.id.buttonGate);

        buttonLights.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, LightsControlActivity.class));
            }
        });

        buttonGate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, GateControlActivity.class));
            }
        });
    }
}
