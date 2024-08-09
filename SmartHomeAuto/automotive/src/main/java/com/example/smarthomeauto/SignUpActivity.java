package com.example.smarthomeauto;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignUpActivity extends AppCompatActivity {

    private AppDatabase db;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usersignup);

        final EditText editTextUsername = findViewById(R.id.editTextSignupUsername);
        final EditText editTextPassword = findViewById(R.id.editTextSignupPassword);
        Button buttonSignUp = findViewById(R.id.buttonSignUp);
        Button buttonBackToLogin = findViewById(R.id.buttonBackToLogin);

        db = AppDatabase.getDatabase(this);
        executorService = Executors.newSingleThreadExecutor();

        buttonSignUp.setOnClickListener(v -> {
            final String username = editTextUsername.getText().toString();
            final String password = editTextPassword.getText().toString();

            if (username.isEmpty() || password.isEmpty()) {
                // Set error messages on the EditText fields
                if (username.isEmpty()) {
                    editTextUsername.setError("Username cannot be empty");
                }
                if (password.isEmpty()) {
                    editTextPassword.setError("Password cannot be empty");
                }
                return;
            }

            executorService.execute(() -> {
                // Check if the user already exists
                boolean userExists = db.userDao().countUsersByUsername(username) > 0;

                if (userExists) {
                    runOnUiThread(() -> showAlertDialog("Error", "Username already exists"));
                } else {
                    // Create and insert the new user
                    User newUser = new User();
                    newUser.Username = username;
                    newUser.Password = HashUtils.hashPassword(password);
                    newUser.Role = "user";
                    newUser.MqttUser = null;
                    newUser.MqttPassword = null;
                    newUser.ManagerUserId = null;
                    newUser.UserBrokerID = null;



                    // Pass the user ID to the next activity
                    Intent intent = new Intent(SignUpActivity.this, UserActivity.class);
                    intent.putExtra("USER_ID", newUser.UserID);
                    intent.putExtra("USER_ROLE", newUser.Role);
                    runOnUiThread(() -> {
                        startActivity(intent);
                        finish(); // Close the sign-up activity
                    });
                }
            });
        });

        buttonBackToLogin.setOnClickListener(v -> {
            // Navigate back to the login screen
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close the sign-up activity
        });
    }

    private void showAlertDialog(String title, String message) {
        new AlertDialog.Builder(SignUpActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (DialogInterface dialog, int which) -> dialog.dismiss())
                .show();
    }
}
