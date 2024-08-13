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
    private String userRole;
    private int specificUserId;

    // Labels that will be hidden or shown
    private View labelManagerUserId, labelBrokerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_user);

        // Initialize views
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextMqttUser = findViewById(R.id.editTextMqttUser);
        editTextMqttPassword = findViewById(R.id.editTextMqttPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerManagerUserId = findViewById(R.id.spinnerManagerUserId);
        spinnerBrokerId = findViewById(R.id.spinnerBrokerId);
        buttonSaveUser = findViewById(R.id.buttonSaveUser);
        buttonBack = findViewById(R.id.buttonBack);

        // Initialize labels that will be shown/hidden
        labelManagerUserId = findViewById(R.id.labelManagerUserId);
        labelBrokerId = findViewById(R.id.labelBrokerId);

        // Initialize database and DAOs
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        userDao = db.userDao();
        brokerDao = db.brokerDao();

        // Retrieve intent extras
        Intent intent = getIntent();
        if (intent != null) {
            userRole = intent.getStringExtra("USER_ROLE");
            Log.d("UserListActivity", "User Role: " + userRole);
        }

        specificUserId = getIntent().getIntExtra("userId", -1);

        // Load data for spinners
        loadRoles();
        loadBrokers();

        // Set onClick listeners
        buttonBack.setOnClickListener(v -> {
            Intent backIntent = new Intent(AddUserActivity.this, UserListActivity.class);
            backIntent.putExtra("USER_ROLE", userRole);
            startActivity(backIntent);
            finish();
        });

        buttonSaveUser.setOnClickListener(v -> saveUser());

        // Handle role selection
        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRole = (String) parent.getItemAtPosition(position);

                if ("admin".equals(selectedRole)) {
                    // Hide broker spinner and its label
                    spinnerBrokerId.setVisibility(View.GONE);
                    labelBrokerId.setVisibility(View.GONE);

                    // Hide MQTT fields and labels
                    editTextMqttUser.setVisibility(View.GONE);
                    editTextMqttPassword.setVisibility(View.GONE);
                    // We assume there's a label for MQTT user and password, which should also be hidden
                    findViewById(R.id.labelMqttUser).setVisibility(View.GONE);
                    findViewById(R.id.labelMqttPassword).setVisibility(View.GONE);

                    // Hide manager spinner and its label
                    labelManagerUserId.setVisibility(View.GONE);
                    spinnerManagerUserId.setVisibility(View.GONE);

                    // Clear values
                    editTextMqttUser.setText(null);
                    editTextMqttPassword.setText(null);
                } else {
                    // Show broker spinner and its label
                    spinnerBrokerId.setVisibility(View.VISIBLE);
                    labelBrokerId.setVisibility(View.VISIBLE);

                    // Show MQTT fields and labels
                    editTextMqttUser.setVisibility(View.VISIBLE);
                    editTextMqttPassword.setVisibility(View.VISIBLE);
                    // Show MQTT labels
                    findViewById(R.id.labelMqttUser).setVisibility(View.VISIBLE);
                    findViewById(R.id.labelMqttPassword).setVisibility(View.VISIBLE);

                    // Show manager spinner and its label if not already visible
                    labelManagerUserId.setVisibility(View.VISIBLE);
                    spinnerManagerUserId.setVisibility(View.VISIBLE);
                }

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
                if ("manager".equals(user.Role)) {
                    managerUsernames.add(user.Username);
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
                        if (user != null && user.UserID == specificUserId) {
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
                if ("user".equals(user.Role)) {
                    userUsernames.add(user.Username);
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
                        if (user != null && user.UserID == specificUserId) {
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
                String description = broker.ClusterUrl + ":" + broker.Port;
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
        } else if (username.length() > 60) {
            editTextUsername.setError("Username must be 60 characters or less");
            hasError = true;
        }
        if (role == null || role.isEmpty()) {
            showAlert("Role is required");
            hasError = true;
        }
        if (!"admin".equals(role)) {
            if (password.isEmpty()) {
                editTextPassword.setError("Password is required");
                hasError = true;
            } else if (password.length() > 60) {
                editTextPassword.setError("Password must be 60 characters or less");
                hasError = true;
            }
            if (mqttUser.isEmpty()) {
                editTextMqttUser.setError("MQTT user is required");
                hasError = true;
            } else if (mqttUser.length() > 40) {
                editTextMqttUser.setError("MQTT user must be 40 characters or less");
                hasError = true;
            }
            if (mqttPassword.isEmpty()) {
                editTextMqttPassword.setError("MQTT password is required");
                hasError = true;
            } else if (mqttPassword.length() > 40) {
                editTextMqttPassword.setError("MQTT password must be 40 characters or less");
                hasError = true;
            }
        }

        if (hasError) {
            return;
        }

        String selectedManagerUsername = (String) spinnerManagerUserId.getSelectedItem();
        String selectedBrokerDescription = (String) spinnerBrokerId.getSelectedItem();

        String brokerURL;
        int brokerPort = -1;

        if (!"admin".equals(role)) {
            if (selectedBrokerDescription != null) {
                String[] parts = selectedBrokerDescription.split(":");
                if (parts.length != 2) {
                    showAlert("Invalid broker format");
                    return;
                }
                brokerURL = parts[0];
                try {
                    brokerPort = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    showAlert("Invalid broker port");
                    return;
                }
            } else {
                brokerURL = null;
                showAlert("Please select a broker");
                return;
            }
        } else {
            brokerURL = null;
        }

        int finalBrokerPort = brokerPort;
        new Thread(() -> {
            User existingUser = userDao.getUserByUsername(username);
            if (existingUser != null) {
                runOnUiThread(() -> showAlert("A user with the same username already exists!"));
            } else {
                User managerUser = userDao.getUserByUsername(selectedManagerUsername);
                Integer managerUserId = ("user".equals(role) || "admin".equals(role)) ? null : (managerUser != null ? managerUser.UserID : null);

                int brokerId = -1;
                if (brokerURL != null && finalBrokerPort != -1) {
                    Broker selectedBroker = brokerDao.getBrokerByUrlAndPort(brokerURL, finalBrokerPort);
                    brokerId = (selectedBroker != null) ? selectedBroker.BrokerID : -1;
                }

                if (brokerId == -1 && !"admin".equals(role)) {
                    runOnUiThread(() -> showAlert("Please select a broker"));
                    return;
                }

                if("user".equals(role)) {
                    int countUsersUsingBroker = userDao.countUsersWithRoleUserByBrokerId(brokerId);
                    Log.d("AddUserActivity", "Broker ID: " + brokerId + " is used by " + countUsersUsingBroker + " users with role 'user'.");

                    if (countUsersUsingBroker > 0) {
                        runOnUiThread(() -> showAlert("This broker is already in use by another user with the role 'user'. Please select a different broker."));
                        return;
                    }
                }
                String hashedPassword = HashUtils.hashPassword(password);

                User newUser = new User(username, hashedPassword, role,
                        "admin".equals(role) ? null : mqttUser,
                        "admin".equals(role) ? null : mqttPassword,
                        "admin".equals(role) ? null :managerUserId,
                        "admin".equals(role) ? null :brokerId);

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


    private void showAlert(String message) {
        new AlertDialog.Builder(AddUserActivity.this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

}
