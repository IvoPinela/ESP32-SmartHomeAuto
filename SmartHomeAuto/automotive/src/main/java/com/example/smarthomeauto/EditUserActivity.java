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
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextMqttUser = findViewById(R.id.editTextMqttUser);
        editTextMqttPassword = findViewById(R.id.editTextMqttPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        spinnerManagerUserId = findViewById(R.id.spinnerManagerUserId);
        spinnerBrokerId = findViewById(R.id.spinnerBrokerId);
        buttonSaveUser = findViewById(R.id.buttonSaveUser);
        buttonBack = findViewById(R.id.buttonBack);

        // Inicializa o banco de dados e os DAOs
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        userDao = db.userDao();
        brokerDao = db.brokerDao();

        // Configura a intenção e o título do formulário
        if (intent.hasExtra("user")) {
            user = (User) intent.getSerializableExtra("user");
            formTitle.setText("Edit User");
            editTextUsername.setText(user.username);
            editTextMqttUser.setText(user.mqttUser);
            editTextMqttPassword.setText(user.mqttPassword);

            // Oculta o campo de senha e o rótulo da senha se estiver editando um usuário existente
            passwordLabel.setVisibility(View.GONE);
            editTextPassword.setVisibility(View.GONE);

            // Carrega outros campos conforme necessário
        } else {
            formTitle.setText("Add New User");
        }

        // Carrega os dados para os spinners
        loadRoles();
        loadManagers();
        loadBrokers();

        // Configura os listeners dos botões
        buttonSaveUser.setOnClickListener(v -> saveUser());

        buttonBack.setOnClickListener(v -> {
            Intent resultIntent = new Intent(EditUserActivity.this, UserListActivity.class);
            resultIntent.putExtra("USER_ROLE", userRole);
            startActivity(resultIntent);
            finish();
        });

        // Configura o listener do Spinner de papéis
        spinnerRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRole = (String) parent.getItemAtPosition(position);
                updateUIForRole(selectedRole);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Implementar caso necessário
            }
        });
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
            updateUIForRole(user.role);
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

    private void updateUIForRole(String role) {
        boolean isAdmin = "admin".equals(role);
        boolean isUserOrGuest = !isAdmin;

        // Atualiza a visibilidade dos campos com base no papel selecionado
        editTextPassword.setVisibility(isUserOrGuest ? View.VISIBLE : View.GONE);
        editTextMqttUser.setVisibility(isUserOrGuest ? View.VISIBLE : View.GONE);
        editTextMqttPassword.setVisibility(isUserOrGuest ? View.VISIBLE : View.GONE);

        spinnerManagerUserId.setVisibility(isUserOrGuest ? View.VISIBLE : View.GONE);
        spinnerBrokerId.setVisibility(isUserOrGuest ? View.VISIBLE : View.GONE);

        // Atualiza a UI para os campos que precisam estar visíveis
        if (isUserOrGuest) {
            loadManagers();
            loadBrokers();
        } else {
            // Se for admin, não precisa carregar managers e brokers
            spinnerManagerUserId.setAdapter(null);
            spinnerBrokerId.setAdapter(null);
        }
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
        if (isFieldVisible(editTextMqttUser) && mqttUser.isEmpty()) {
            editTextMqttUser.setError("MQTT user is required");
            hasError = true;
        }
        if (isFieldVisible(editTextMqttPassword) && mqttPassword.isEmpty()) {
            editTextMqttPassword.setError("MQTT password is required");
            hasError = true;
        }
        if (role == null || role.isEmpty()) {
            showAlert("Role is required");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        Integer managerUserId;
        Integer brokerId;

        if (!"admin".equals(role)) {
            if (selectedBrokerDescription != null && !selectedBrokerDescription.isEmpty()) {
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

                Broker selectedBroker = brokerDao.getBrokerByUrlAndPort(brokerURL, brokerPort);
                brokerId = (selectedBroker != null) ? selectedBroker.PK_BrokerID : -1;

                if (brokerId == -1) {
                    runOnUiThread(() -> showAlert("Please select a broker"));
                    return;
                }
            } else {
                brokerId = null;
            }

            User managerUser = userDao.getUserByUsername(selectedManagerUsername);
            managerUserId = (managerUser != null) ? managerUser.id : null;
        } else {
            brokerId = null;
            managerUserId = null;
        }

        new Thread(() -> {
            User existingUser = userDao.getUserByUsername(username);
            if (existingUser != null && (user == null || existingUser.id != user.id)) {
                runOnUiThread(() -> showAlert("A user with the same username already exists!"));
            } else {
                if (user != null) {
                    user.username = username;
                    user.password = password;
                    user.mqttUser = mqttUser;
                    user.mqttPassword = mqttPassword;
                    user.role = role;
                    user.managerUserId = managerUserId;
                    user.brokerID = brokerId;

                    userDao.update(user);
                } else {
                    User newUser = new User(username, password, mqttUser, mqttPassword, role, managerUserId, brokerId);
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

    private boolean isFieldVisible(View view) {
        return view.getVisibility() == View.VISIBLE;
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
