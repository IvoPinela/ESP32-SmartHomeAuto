package com.example.smarthomeauto;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;

public class EditUserActivity extends Activity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private EditText editTextMqttUser;
    private EditText editTextMqttPassword;
    private Spinner spinnerRole;
    private Spinner spinnerManagerUserId;
    private Spinner spinnerBrokerId;
    private Button buttonSaveUser;
    private Button buttonBack;
    private User user;
    private UserDao userDao;
    private BrokerDao brokerDao;

    private String userRole;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_user);

        Intent intent = getIntent();
        if (intent != null) {
            userRole = intent.getStringExtra("USER_ROLE");
            Log.d("EditUserActivity", "User Role: " + userRole);
        }

        // Inicializa os componentes da interface
        TextView formTitle = findViewById(R.id.formTitleUser);
        TextView passwordLabel = findViewById(R.id.labelPassword);
        TextView mqttUserLabel = findViewById(R.id.labelMqttUser);
        TextView mqttPasswordLabel = findViewById(R.id.labelMqttPassword);
        TextView managerUserIdLabel = findViewById(R.id.labelManagerUserId);
        TextView brokerIdLabel = findViewById(R.id.labelBrokerId);

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

        if (intent.hasExtra("user")) {
            user = (User) intent.getSerializableExtra("user");
            formTitle.setText("Edit User");
            editTextUsername.setText(user.username);
            editTextMqttUser.setText(user.mqttUser);
            editTextMqttPassword.setText(user.mqttPassword);

            // Manter password invisível
            passwordLabel.setVisibility(View.GONE);
            editTextPassword.setVisibility(View.GONE);
        } else {
            formTitle.setText("Add New User");
        }

        // Carrega os dados para os spinners
        loadRoles();
        loadManagers();
        loadBrokers();

        buttonSaveUser.setOnClickListener(v -> saveUser());

        buttonBack.setOnClickListener(v -> {
            Intent resultIntent = new Intent(EditUserActivity.this, UserListActivity.class);
            resultIntent.putExtra("USER_ROLE", userRole);
            startActivity(resultIntent);
            finish();
        });

        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRole = (String) parent.getItemAtPosition(position);

                if ("admin".equals(selectedRole)) {
                    // Ocultar campos para MQTT, manager e broker
                    hideNonAdminFields();
                } else {
                    // Mostrar todos os campos
                    showAllFields();
                    if ("user".equals(selectedRole)) {
                        ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(EditUserActivity.this, android.R.layout.simple_spinner_item, new ArrayList<>());
                        emptyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerManagerUserId.setAdapter(emptyAdapter);
                    } else if ("guest".equals(selectedRole)) {
                        loadUsersWithRoleUser();
                    } else {
                        loadManagers();
                    }
                }

                if (user != null) {
                    spinnerRole.setSelection(getRoleIndex(user.role));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Optional: Handle case where nothing is selected
            }
        });
    }

    private void hideNonAdminFields() {
        spinnerBrokerId.setVisibility(View.GONE);
        findViewById(R.id.labelBrokerId).setVisibility(View.GONE);
        editTextMqttUser.setVisibility(View.GONE);
        editTextMqttPassword.setVisibility(View.GONE);
        findViewById(R.id.labelMqttUser).setVisibility(View.GONE);
        findViewById(R.id.labelMqttPassword).setVisibility(View.GONE);
        spinnerManagerUserId.setVisibility(View.GONE);
        findViewById(R.id.labelManagerUserId).setVisibility(View.GONE);
    }

    private void showAllFields() {
        spinnerBrokerId.setVisibility(View.VISIBLE);
        findViewById(R.id.labelBrokerId).setVisibility(View.VISIBLE);
        editTextMqttUser.setVisibility(View.VISIBLE);
        editTextMqttPassword.setVisibility(View.VISIBLE);
        findViewById(R.id.labelMqttUser).setVisibility(View.VISIBLE);
        findViewById(R.id.labelMqttPassword).setVisibility(View.VISIBLE);
        spinnerManagerUserId.setVisibility(View.VISIBLE);
        findViewById(R.id.labelManagerUserId).setVisibility(View.VISIBLE);
    }

    private int getRoleIndex(String role) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerRole.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (role.equals(adapter.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private void loadRoles() {
        List<String> roles = new ArrayList<>();
        roles.add("user");
        roles.add("guest");
        if ("adminmaster".equals(userRole)) {
            roles.add("admin");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

        if (user != null) {
            for (int i = 0; i < spinnerRole.getCount(); i++) {
                if (user.role.equals(spinnerRole.getItemAtPosition(i))) {
                    spinnerRole.setSelection(i);
                    break;
                }
            }
        }
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
                ArrayAdapter<String> adapter = new ArrayAdapter<>(EditUserActivity.this, android.R.layout.simple_spinner_item, managerUsernames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerManagerUserId.setAdapter(adapter);

                if (user != null && user.managerUserId != null) {
                    for (int i = 0; i < spinnerManagerUserId.getCount(); i++) {
                        final int index = i;
                        String username = (String) spinnerManagerUserId.getItemAtPosition(index);
                        new Thread(() -> {
                            User managerUser = userDao.getUserByUsername(username);
                            runOnUiThread(() -> {
                                if (managerUser != null && managerUser.id == user.managerUserId) {
                                    spinnerManagerUserId.setSelection(index);
                                }
                            });
                        }).start();
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
                ArrayAdapter<String> adapter = new ArrayAdapter<>(EditUserActivity.this, android.R.layout.simple_spinner_item, userUsernames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerManagerUserId.setAdapter(adapter);

                if (user != null && user.managerUserId != null) {
                    for (int i = 0; i < spinnerManagerUserId.getCount(); i++) {
                        final int index = i;
                        String username = (String) spinnerManagerUserId.getItemAtPosition(index);
                        new Thread(() -> {
                            User managerUser = userDao.getUserByUsername(username);
                            runOnUiThread(() -> {
                                if (managerUser != null && managerUser.id == user.managerUserId) {
                                    spinnerManagerUserId.setSelection(index);
                                }
                            });
                        }).start();
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
                ArrayAdapter<String> adapter = new ArrayAdapter<>(EditUserActivity.this, android.R.layout.simple_spinner_item, brokerDescriptions);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerBrokerId.setAdapter(adapter);

                if (user != null && user.brokerID != null) {
                    for (int i = 0; i < spinnerBrokerId.getCount(); i++) {
                        final int index = i;
                        String description = (String) spinnerBrokerId.getItemAtPosition(index);
                        String[] parts = description.split(":");
                        if (parts.length == 2) {
                            String brokerURL = parts[0];
                            int brokerPort;
                            try {
                                brokerPort = Integer.parseInt(parts[1]);
                            } catch (NumberFormatException e) {
                                runOnUiThread(() -> showAlert("Invalid broker port"));
                                return;
                            }
                            new Thread(() -> {
                                Broker broker = brokerDao.getBrokerByUrlAndPort(brokerURL, brokerPort);
                                runOnUiThread(() -> {
                                    if (broker != null && broker.PK_BrokerID == user.brokerID) {
                                        spinnerBrokerId.setSelection(index);
                                    }
                                });
                            }).start();
                        }
                    }
                }
            });
        }).start();
    }

    private void saveUser() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String mqttUser = editTextMqttUser.getText().toString().trim();
        String mqttPassword = editTextMqttPassword.getText().toString().trim();
        String role = (String) spinnerRole.getSelectedItem();
        String selectedManagerUsername = (String) spinnerManagerUserId.getSelectedItem();
        String selectedBrokerDescription = (String) spinnerBrokerId.getSelectedItem();

        boolean hasError = false;

        if (username.isEmpty()) {
            editTextUsername.setError("Username is required");
            hasError = true;
        }
        if (mqttUser.isEmpty() && !"admin".equals(role)) {
            editTextMqttUser.setError("MQTT user is required");
            hasError = true;
        }
        if (mqttPassword.isEmpty() && !"admin".equals(role)) {
            editTextMqttPassword.setError("MQTT password is required");
            hasError = true;
        }
        if (role == null || role.isEmpty()) {
            showAlert("Role is required");
            hasError = true;
        }
        if ("guest".equals(role) && selectedManagerUsername == null) {
            showAlert("Manager selection is required for guests");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        String[] parts = selectedBrokerDescription != null ? selectedBrokerDescription.split(":") : new String[0];
        if (parts.length != 2 && !"admin".equals(role)) {
            showAlert("Invalid broker format");
            return;
        }

        String brokerURL = parts.length == 2 ? parts[0] : "";
        int brokerPort = -1;
        try {
            brokerPort = parts.length == 2 ? Integer.parseInt(parts[1]) : -1;
        } catch (NumberFormatException e) {
            showAlert("Invalid broker port");
            return;
        }

        int finalBrokerPort = brokerPort;
        new Thread(() -> {
            User existingUser = userDao.getUserByUsername(username);
            if (existingUser != null && (user == null || existingUser.id != user.id)) {
                runOnUiThread(() -> showAlert("A user with the same username already exists!"));
            } else {
                User managerUser = ("guest".equals(role)) ? userDao.getUserByUsername(selectedManagerUsername) : null;
                Integer managerUserId = ("guest".equals(role)) ? (managerUser != null ? managerUser.id : null) : null;

                Broker selectedBroker = ("admin".equals(role)) ? null : brokerDao.getBrokerByUrlAndPort(brokerURL, finalBrokerPort);
                int brokerId = (selectedBroker != null) ? selectedBroker.PK_BrokerID : -1;

                if (brokerId == -1 && !"admin".equals(role)) {
                    runOnUiThread(() -> showAlert("Please select a broker"));
                    return;
                }

                if (managerUserId == null && "guest".equals(role)) {
                    runOnUiThread(() -> showAlert("Please select a manager"));
                    return;
                }

                if (user != null) {
                    user.username = username;
                    user.password = password.isEmpty() ? user.password : password;
                    user.mqttUser = "admin".equals(role) ? null : mqttUser;
                    user.mqttPassword = "admin".equals(role) ? null : mqttPassword;
                    user.role = role;
                    user.managerUserId = ("guest".equals(role)) ? managerUserId : null;
                    user.brokerID = ("admin".equals(role)) ? null : brokerId;

                    userDao.update(user);
                } else {
                    User newUser = new User(username, password,
                            "admin".equals(role) ? null : mqttUser,
                            "admin".equals(role) ? null : mqttPassword,
                            role,
                            ("guest".equals(role)) ? managerUserId : null,
                            ("admin".equals(role)) ? null : brokerId);
                    userDao.insert(newUser);
                }

                runOnUiThread(() -> {
                    showAlert("User saved!");
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("user", user);
                    resultIntent.putExtra("USER_ROLE", userRole);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
            }
        }).start();
    }


    private void showAlert(String message) {
        new AlertDialog.Builder(EditUserActivity.this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}
