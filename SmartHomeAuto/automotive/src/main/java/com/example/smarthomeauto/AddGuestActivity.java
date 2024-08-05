package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

public class AddGuestActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword;
    private Button buttonSaveGuest, buttonBack;
    private UserDao userDao;
    private String userRole;
    private int managerUserId;
    private MqttManager mqttManager;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_guest);

        rootView = findViewById(android.R.id.content);

        // Inicializa as views
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonSaveGuest = findViewById(R.id.buttonSaveUser);
        buttonBack = findViewById(R.id.buttonBack);

        // Inicializa o banco de dados e DAO
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        userDao = db.userDao();

        // Recupera extras da intent
        Intent intent = getIntent();
        if (intent != null) {
            userRole = intent.getStringExtra("USER_ROLE");
            managerUserId = intent.getIntExtra("USER_ID", -1);
            Log.d("AddGuestActivity", "User Role: " + userRole);
            Log.d("AddGuestActivity", "Manager User ID: " + managerUserId);
        }
        mqttManager = new MqttManager(this, managerUserId);
        // Define os listeners dos botões
        buttonSaveGuest.setOnClickListener(v -> saveGuest());

        buttonBack.setOnClickListener(v -> {
            Intent backIntent = new Intent(AddGuestActivity.this, GuestListActivity.class);
            backIntent.putExtra("USER_ROLE", userRole);
            backIntent.putExtra("USER_ID",managerUserId);
            startActivity(backIntent);
            finish();
        });
    }

    private void saveGuest() {
        boolean hasError = false;
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (username.isEmpty()) {
            editTextUsername.setError("Username is required");
            hasError = true;
        }
        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        new Thread(() -> {
            User existingUser = userDao.getUserByUsername(username);
            if (existingUser != null) {
                runOnUiThread(() -> showAlert("A user with the same username already exists!"));
            } else {
                String hashedPassword = HashUtils.hashPassword(password);
                // Obtém o brokerID baseado no managerUserId, se necessário
                int brokerId=-1;
                if (managerUserId != -1) {
                     brokerId = userDao.getBrokerById(managerUserId);
                    Log.d("AddGuestActivity", "Broker ID obtained for managerUserId " + managerUserId + ": " + brokerId);
                }else {
                    Log.d("AddGuestActivity", "ManagerUserId is -1, no broker ID obtained.");
                }


                User newUser = new User(username, hashedPassword, "guest", null, null,managerUserId , brokerId);

                try {
                    userDao.insert(newUser);
                    runOnUiThread(() -> {
                        showAlert("Guest saved!");
                        Intent intent = new Intent(AddGuestActivity.this, UserListActivity.class);
                        intent.putExtra("USER_ROLE", userRole);
                        setResult(RESULT_OK, intent);
                        finish();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> showAlert("Failed to save guest: " + e.getMessage()));
                }
            }
        }).start();
    }


    private void showAlert(String message) {
        new AlertDialog.Builder(AddGuestActivity.this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttManager != null) {
            mqttManager.disconnect();
        }
    }
}
