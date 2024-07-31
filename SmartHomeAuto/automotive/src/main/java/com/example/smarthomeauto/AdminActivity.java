package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AdminActivity extends AppCompatActivity {

    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adminscreen);

        Button buttonShowDevices = findViewById(R.id.buttonShowDevices);
        Button buttonLogOff = findViewById(R.id.buttonLogOff);
        Button buttonShowBrokers = findViewById(R.id.buttonShowBrokers);
        Button buttonShowUsers = findViewById(R.id.buttonShowUsers);


        Intent intent = getIntent();
        if (intent != null) {
            userRole = intent.getStringExtra("USER_ROLE");
            Log.d("AdminActivity", "User Role: " + userRole);
        }




        buttonLogOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Log off and return to login screen
                Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Close the AdminActivity
            }
        });
        buttonShowDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start DeviceListActivity
                Intent intent = new Intent(AdminActivity.this, DeviceListActivity.class);
                startActivity(intent);
            }
        });
        buttonShowBrokers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start BrokerListActivity
                Intent intent = new Intent(AdminActivity.this, BrokerListActivity.class);
                startActivity(intent);
            }
        });
        buttonShowUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start UserListActivity
                Intent intent = new Intent(AdminActivity.this, UserListActivity.class);
                intent.putExtra("USER_ROLE", userRole);
                startActivity(intent);
            }
        });
    }
}
