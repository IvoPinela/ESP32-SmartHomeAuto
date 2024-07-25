package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AdminActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adminscreen);

        Button buttonAddAdmin = findViewById(R.id.buttonAddAdmin);
        Button buttonLogOff = findViewById(R.id.buttonLogOff);

        buttonAddAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start SignUpActivity with admin role
                Intent intent = new Intent(AdminActivity.this, SignUpActivity.class);
                intent.putExtra("USER_ROLE", "admin"); // Pass the admin role
                startActivity(intent);
            }
        });

        buttonLogOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Log off and return to login screen
                Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Close the AdminActivity
            }
        });
    }
}
