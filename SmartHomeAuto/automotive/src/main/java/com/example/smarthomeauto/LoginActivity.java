package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private AppDatabase db;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final EditText editTextUsername = findViewById(R.id.editTextUsername);
        final EditText editTextPassword = findViewById(R.id.editTextPassword);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonSignUp = findViewById(R.id.buttonSignUp);

        db = AppDatabase.getDatabase(this);
        executorService = Executors.newSingleThreadExecutor();

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = editTextUsername.getText().toString();
                final String password = editTextPassword.getText().toString();

                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        User user = db.userDao().getUserByCredentials(username, HashUtils.hashPassword(password));

                        if (user != null) {
                            Log.d("login", "User found: " + user.username);
                            Log.d("login", "User role: " + user.role);

                            Log.d("login", "Password match successful");
                            Intent intent;
                            if ("admin".equals(user.role)) {
                                intent = new Intent(LoginActivity.this, AdminActivity.class);
                            } else if ("user".equals(user.role)) {
                                intent = new Intent(LoginActivity.this, UserActivity.class);
                            } else {
                                Log.d("login", "Unknown user role");
                                return;
                            }
                            startActivity(intent);
                        } else {
                            Log.d("login", "No user found with the given credentials");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showSnackbar(v, "Invalid user or password");
                                }
                            });
                        }
                    }
                });
            }
        });

        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Sign Up Activity with user role
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                intent.putExtra("USER_ROLE", "user");
                startActivity(intent);
            }
        });
    }

    private void showSnackbar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }
}
