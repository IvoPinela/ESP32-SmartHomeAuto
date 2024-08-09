package com.example.smarthomeauto;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.room.Room;

public class EditGuestActivity extends Activity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button buttonSaveGuest;
    private Button buttonBack;
    private User guest;
    private UserDao userDao;
    private TextView textViewTitle;
    private int managerUserId;
    private int brokerId;
    private String userRole;
    private MqttManager mqttManager;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_guest);

        rootView = findViewById(android.R.id.content);

        textViewTitle = findViewById(R.id.formTitleUser);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSaveGuest = findViewById(R.id.buttonSaveUser);
        buttonBack = findViewById(R.id.buttonBack);

        userDao = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build().userDao();

        Intent intent = getIntent();
        managerUserId = intent.getIntExtra("USER_ID", -1);
        userRole = intent.getStringExtra("USER_ROLE");

        mqttManager = new MqttManager(this, managerUserId);
        if (intent.hasExtra("user")) {
            guest = (User) intent.getSerializableExtra("user");
            textViewTitle.setText("Edit Guest");
            editTextUsername.setText(guest.Username);
            editTextPassword.setText(""); // Mantém o campo vazio
        } else {
            textViewTitle.setText("Add Guest");
        }

        buttonSaveGuest.setOnClickListener(v -> saveGuest());
        buttonBack.setOnClickListener(v -> {
            Intent backIntent = new Intent(EditGuestActivity.this, GuestListActivity.class);
            backIntent.putExtra("USER_ROLE", userRole);
            backIntent.putExtra("USER_ID", managerUserId);
            startActivity(backIntent);
            finish();
        });
    }

    private void saveGuest() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty()) {
            editTextUsername.setError("Username is required");
            return;
        }

        new Thread(() -> {
            User existingUser = userDao.getUserByUsername(username);
            if (existingUser != null && (guest == null || existingUser.UserID != guest.UserID)) {
                runOnUiThread(() -> Toast.makeText(EditGuestActivity.this, "A user with the same username already exists!", Toast.LENGTH_SHORT).show());
                return;
            }

            if (guest != null) {
                guest.Username = username;

                if (!password.isEmpty()) {
                    guest.Password = HashUtils.hashPassword(password);
                } else {
                    // Mantém a senha existente sem alteração
                    guest.Password = existingUser.Password;
                }
                userDao.update(guest);
            } else {
                brokerId = userDao.getBrokerById(managerUserId);
                String hashedPassword = HashUtils.hashPassword(password);
                guest = new User(username, hashedPassword, null, null, null, managerUserId, brokerId);
                userDao.insert(guest);
            }

            runOnUiThread(() -> {
                Toast.makeText(EditGuestActivity.this, "Guest saved!", Toast.LENGTH_SHORT).show();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("user", guest);
                resultIntent.putExtra("USER_ROLE", userRole);
                resultIntent.putExtra("USER_ID", managerUserId);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        }).start();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttManager != null) {
            mqttManager.disconnect();
        }
    }
}
