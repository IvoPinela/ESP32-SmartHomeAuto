package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final int MAX_ATTEMPTS = 5;
    private AppDatabase db;
    private ExecutorService executorService;
    private int loginAttempts = 0;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText editTextUsername = findViewById(R.id.editTextUsername);
        final EditText editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonSignUp = findViewById(R.id.buttonSignUp);

        db = AppDatabase.getDatabase(this);
        executorService = Executors.newSingleThreadExecutor();

        buttonLogin.setOnClickListener(v -> {
            if (loginAttempts >= MAX_ATTEMPTS) {
                showSnackbar(v, "Account locked due to too many failed login attempts.");
                return;
            }

            final String username = editTextUsername.getText().toString();
            final String password = editTextPassword.getText().toString();

            executorService.execute(() -> {
                User user = db.userDao().getUserByCredentials(username, HashUtils.hashPassword(password));

                if (user != null) {
                    Log.d("login", "User found: " + user.username);
                    Log.d("login", "User role: " + user.role);

                    // Reset attempts on successful login
                    loginAttempts = 0;

                    Intent intent;
                    if ("admin".equals(user.role) || "adminmaster".equals(user.role)) {
                        intent = new Intent(LoginActivity.this, AdminActivity.class);
                    } else if ("user".equals(user.role)||("guest".equals(user.role))) {
                        intent = new Intent(LoginActivity.this, UserActivity.class);
                    } else {
                        Log.d("login", "Unknown user role");
                        return;
                    }

                    // Pass both the user ID and role to the next activity
                    intent.putExtra("USER_ID", user.id);
                    intent.putExtra("USER_ROLE", user.role);
                    startActivity(intent);
                } else {
                    loginAttempts++;

                    Log.d("login", "No user found with the given credentials");
                    runOnUiThread(() -> {
                        String errorMessage = "Invalid user or password";
                        if (loginAttempts > MAX_ATTEMPTS) {
                            errorMessage = "Account locked due to too many failed login attempts.";
                        } else if (MAX_ATTEMPTS-loginAttempts <= 3 && MAX_ATTEMPTS-loginAttempts >0) {
                            showAttemptsWarningDialog(MAX_ATTEMPTS-loginAttempts);
                        }

                        editTextUsername.setError("UserName or Password Incorrect");
                        editTextPassword.setError("UserName or Password Incorrect ");
                        showSnackbar(v, errorMessage);
                    });
                }
            });
        });

        buttonSignUp.setOnClickListener(v -> {
            // Navigate to Sign Up Activity
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void showSnackbar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }
    private void showAttemptsWarningDialog(int remainingAttempts) {
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage("You have " + remainingAttempts + " attempts remaining before your account is locked.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

}
