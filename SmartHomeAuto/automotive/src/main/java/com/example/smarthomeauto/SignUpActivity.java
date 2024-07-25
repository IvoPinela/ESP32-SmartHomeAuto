package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignUpActivity extends AppCompatActivity {

    private AppDatabase db;
    private ExecutorService executorService;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the user role from the intent
        userRole = getIntent().getStringExtra("USER_ROLE");

        // Set the appropriate layout based on user role
        if ("admin".equals(userRole)) {
            setContentView(R.layout.adminsignup);
        } else {
            setContentView(R.layout.usersignup);
        }

        final EditText editTextUsername = findViewById(R.id.editTextSignupUsername);
        final EditText editTextPassword = findViewById(R.id.editTextSignupPassword);
        Button buttonSignUp = findViewById(R.id.buttonSignUp);
        Button buttonBackToLogin = findViewById(R.id.buttonBackToLogin);

        db = AppDatabase.getDatabase(this);
        executorService = Executors.newSingleThreadExecutor();

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = editTextUsername.getText().toString();
                final String password = editTextPassword.getText().toString();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "Username and password cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }

                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        // Check if the user already exists
                        if (db.userDao().countUsersByUsername(username) > 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(SignUpActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;
                        }

                        // Create and insert the new user
                        User newUser = new User();
                        newUser.username = username;
                        newUser.password = HashUtils.hashPassword(password); // Hash the password
                        newUser.role = userRole; // Set the role based on intent

                        db.userDao().insert(newUser);
                        Log.d("signup", userRole + " user created successfully");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SignUpActivity.this, userRole + " sign up successful", Toast.LENGTH_SHORT).show();
                                // Redirect based on user role
                                Intent intent;
                                if ("admin".equals(userRole)) {
                                    // Redirect to admin activity if the role is admin
                                    intent = new Intent(SignUpActivity.this, AdminActivity.class);
                                } else {
                                    // Redirect to user activity if the role is user
                                    intent = new Intent(SignUpActivity.this, UserActivity.class);
                                }
                                startActivity(intent);
                                finish(); // Close the sign-up activity
                            }
                        });
                    }
                });
            }
        });

        buttonBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to the login screen
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Close the sign-up activity
            }
        });
    }
}
