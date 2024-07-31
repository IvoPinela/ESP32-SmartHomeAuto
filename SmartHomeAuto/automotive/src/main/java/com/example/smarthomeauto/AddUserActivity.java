package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import java.util.ArrayList;
import java.util.List;

public class AddUserActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword, editTextMqttUser, editTextMqttPassword;
    private Spinner spinnerManagerUserId, spinnerBrokerId, spinnerRole;
    private Button buttonSaveUser, buttonBack;
    private UserDao userDao;
    private BrokerDao brokerDao;
    private String currentUserRole;
    private int specificUserId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent2 = getIntent();
        if (intent2 != null) {
            userRole = intent2.getStringExtra("USER_ROLE");
            Log.d("UserListActivity", "User Role: " + userRole);
        }

        setContentView(R.layout.add_edit_user);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextMqttUser = findViewById(R.id.editTextMqttUser);
        editTextMqttPassword = findViewById(R.id.editTextMqttPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerManagerUserId = findViewById(R.id.spinnerManagerUserId);
        spinnerBrokerId = findViewById(R.id.spinnerBrokerId);
        buttonSaveUser = findViewById(R.id.buttonSaveUser);
        buttonBack = findViewById(R.id.buttonBack);

        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        userDao = db.userDao();
        brokerDao = db.brokerDao();

        specificUserId = getIntent().getIntExtra("userId", -1);
        currentUserRole = getIntent().getStringExtra("currentUserRole");

        loadRoles();
        loadBrokers();

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(AddUserActivity.this, UserListActivity.class);
            intent.putExtra("USER_ROLE", userRole);
            startActivity(intent);
            finish();
        });

        buttonSaveUser.setOnClickListener(v -> saveUser());

        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRole = (String) parent.getItemAtPosition(position);
                handleRoleSelection(selectedRole);
                if ("user".equals(selectedRole)) {
                    ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(AddUserActivity.this, android.R.layout.simple_spinner_item, new ArrayList<>());
                    emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerManagerUserId.setAdapter(emptyAdapter);
                } else if ("guest".equals(selectedRole)) {
                    loadUsersWithRoleUser();
                } else {
                    loadManagers();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Optional: Handle case where nothing is selected
            }
        });

        if (specificUserId != -1) {
            loadManagers();
        }
    }

    private void loadRoles() {
        List<String> roles = new ArrayList<>();
        roles.add("user");
        roles.add("guest");
        if (userRole.equals("adminmaster")) {
            roles.add("admin");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }

    private void loadManagers() {
        new Thread(() -> {
            List<User> managers = userDao.getAllUsers();
            List<String> managerUsernames = new ArrayList<>();
            for (User user : managers) {
                if ("manager".equals(user.role)) {
                    managerUsernames.add(user.username);
                }
            }
            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AddUserActivity.this, android.R.layout.simple_spinner_item, managerUsernames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerManagerUserId.setAdapter(adapter);

                if (specificUserId != -1) {
                    for (int i = 0; i < spinnerManagerUserId.getCount(); i++) {
                        String username = (String) spinnerManagerUserId.getItemAtPosition(i);
                        User user = userDao.getUserByUsername(username);
                        if (user != null && user.id == specificUserId) {
                            spinnerManagerUserId.setSelection(i);
                            break;
                        }
                    }
                }
            });
        }).start();
    }

    private void loadUsersWithRoleUser() {
        new Thread(() -> {
            List<User> users = userDao.getAllUsers();
            List<String> userUsernames = new ArrayList<>();
            for (User user : users) {
                if ("user".equals(user.role)) {
                    userUsernames.add(user.username);
                }
            }
            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AddUserActivity.this, android.R.layout.simple_spinner_item, userUsernames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerManagerUserId.setAdapter(adapter);

                if (specificUserId != -1) {
                    for (int i = 0; i < spinnerManagerUserId.getCount(); i++) {
                        String username = (String) spinnerManagerUserId.getItemAtPosition(i);
                        User user = userDao.getUserByUsername(username);
                        if (user != null && user.id == specificUserId) {
                            spinnerManagerUserId.setSelection(i);
                            break;
                        }
                    }
                }
            });
        }).start();
    }

    private void loadBrokers() {
        new Thread(() -> {
            List<Broker> brokers = brokerDao.getAllBrokers();
            List<String> brokerDescriptions = new ArrayList<>();
            for (Broker broker : brokers) {
                String description = broker.ClusterURL + ":" + broker.PORT;
                brokerDescriptions.add(description);
            }
            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AddUserActivity.this, android.R.layout.simple_spinner_item, brokerDescriptions);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerBrokerId.setAdapter(adapter);
            });
        }).start();
    }

    private void saveUser() {
        boolean hasError = false;
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String mqttUser = editTextMqttUser.getText().toString().trim();
        String mqttPassword = editTextMqttPassword.getText().toString().trim();
        String role = (String) spinnerRole.getSelectedItem();

        if (username.isEmpty()) {
            editTextUsername.setError("Username is required");
            hasError = true;
        }
        if (password.isEmpty()) {
            editTextPassword.setError("Password is required");
            hasError = true;
        }
        if (role == null || role.isEmpty()) {
            showAlert("Role is required");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        // If the role is "admin", set brokerId to null
        String selectedManagerUsername = "admin".equals(role) ? null : (String) spinnerManagerUserId.getSelectedItem();
        String selectedBrokerDescription = "admin".equals(role) ? null : (String) spinnerBrokerId.getSelectedItem();

        Integer brokerId;
        if (selectedBrokerDescription != null) {
            // Process broker details only if a broker is selected
            String[] parts = selectedBrokerDescription.split(":");
            if (parts.length != 2) {
                showAlert("Invalid broker format");
                return;
            }

            String brokerURL = parts[0];
            int brokerPort;
            try {
                brokerPort = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                showAlert("Invalid broker port");
                return;
            }

            // Find brokerId based on URL and port
            Broker selectedBroker = brokerDao.getBrokerByUrlAndPort(brokerURL, brokerPort);
            brokerId = (selectedBroker != null) ? selectedBroker.PK_BrokerID : null;

            if (brokerId == null) {
                showAlert("Please select a valid broker");
                return;
            }
        } else {
            brokerId = null;
        }

        new Thread(() -> {
            User existingUser = userDao.getUserByUsername(username);
            if (existingUser != null) {
                runOnUiThread(() -> showAlert("A user with the same username already exists!"));
            } else {
                User managerUser = selectedManagerUsername != null ? userDao.getUserByUsername(selectedManagerUsername) : null;
                Integer managerUserId = managerUser != null ? managerUser.id : null;

                // Hash the password before storing
                String hashedPassword = HashUtils.hashPassword(password);

                User newUser = new User(username, hashedPassword, role, mqttUser, mqttPassword, managerUserId, brokerId);

                try {
                    userDao.insert(newUser);
                    runOnUiThread(() -> {
                        showAlert("User saved!");
                        Intent intent = new Intent(AddUserActivity.this, UserListActivity.class);
                        intent.putExtra("USER_ROLE", userRole);
                        setResult(RESULT_OK, intent);
                        finish();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> showAlert("Failed to save user: " + e.getMessage()));
                }
            }
        }).start();
    }


    private void handleRoleSelection(String selectedRole) {
        boolean isAdmin = "admin".equals(selectedRole);

        // Show/Hide fields based on role
        findViewById(R.id.spinnerManagerUserId).setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        findViewById(R.id.spinnerBrokerId).setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        findViewById(R.id.editTextMqttUser).setVisibility(isAdmin ? View.GONE : View.VISIBLE);
        findViewById(R.id.editTextMqttPassword).setVisibility(isAdmin ? View.GONE : View.VISIBLE);
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(AddUserActivity.this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}
