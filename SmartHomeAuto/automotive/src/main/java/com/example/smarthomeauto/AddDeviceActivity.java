package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;

public class AddDeviceActivity extends AppCompatActivity {

    private EditText editTextDeviceName, editTextMqttTopic, editTextMqttUser, editTextMqttPassword;
    private Spinner spinnerDeviceType, spinnerCreatorUser;
    private Button buttonSaveDevice, buttonBack;
    private DeviceDao deviceDao;
    private DeviceTypeDao deviceTypeDao;
    private UserDao userDao;
    private int specificUserId;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        Intent intent2 = getIntent();
        if (intent2 != null) {
            userRole = intent2.getStringExtra("USER_ROLE");
            Log.d("UserListActivity", "User Role: " + userRole);
        }
        editTextDeviceName = findViewById(R.id.editTextDeviceName);
        editTextMqttTopic = findViewById(R.id.editTextMqttTopic);
        editTextMqttUser = findViewById(R.id.editTextMqttUser);
        editTextMqttPassword = findViewById(R.id.editTextMqttPassword);
        spinnerDeviceType = findViewById(R.id.spinnerDeviceType);
        spinnerCreatorUser = findViewById(R.id.spinnerCreatorUser);
        buttonSaveDevice = findViewById(R.id.buttonSaveDevice);
        buttonBack = findViewById(R.id.buttonBack);

        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        deviceDao = db.deviceDao();
        deviceTypeDao = db.deviceTypeDao();
        userDao = db.userDao();

        specificUserId = getIntent().getIntExtra("userId", -1);

        loadDeviceTypes();
        loadUsers();

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(AddDeviceActivity.this, DeviceListActivity.class);
            intent.putExtra("USER_ROLE", userRole);
            startActivity(intent);
            finish();
        });

        buttonSaveDevice.setOnClickListener(v -> saveDevice());
    }

    private void loadDeviceTypes() {
        new Thread(() -> {
            List<DeviceType> deviceTypes = deviceTypeDao.getAllDeviceTypes();
            runOnUiThread(() -> {
                ArrayAdapter<DeviceType> adapter = new ArrayAdapter<>(AddDeviceActivity.this, android.R.layout.simple_spinner_item, deviceTypes);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDeviceType.setAdapter(adapter);
            });
        }).start();
    }

    private void loadUsers() {
        new Thread(() -> {
            List<User> users = userDao.getAllUsers();
            List<User> filteredUsers = new ArrayList<>();
            for (User user : users) {
                if ("user".equals(user.role)) {
                    filteredUsers.add(user);
                }
            }
            runOnUiThread(() -> {
                ArrayAdapter<User> adapter = new ArrayAdapter<>(AddDeviceActivity.this, android.R.layout.simple_spinner_item, filteredUsers);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCreatorUser.setAdapter(adapter);

                if (specificUserId != -1) {
                    for (int i = 0; i < spinnerCreatorUser.getCount(); i++) {
                        User user = (User) spinnerCreatorUser.getItemAtPosition(i);
                        if (user.id == specificUserId) {
                            spinnerCreatorUser.setSelection(i);
                            break;
                        }
                    }
                }
            });
        }).start();
    }

    private void saveDevice() {

        boolean hasError = false;
        String name = editTextDeviceName.getText().toString().trim();
        String topic = editTextMqttTopic.getText().toString().trim();
        String user = editTextMqttUser.getText().toString().trim();
        String password = editTextMqttPassword.getText().toString().trim();

        // Validate fields
        if (name.isEmpty()) {
            editTextDeviceName.setError("Device name is required");
            hasError = true;
        }
        if (topic.isEmpty()) {
            editTextMqttTopic.setError("MQTT topic is required");
            hasError = true;
        }
        if (user.isEmpty()) {
            editTextMqttUser.setError("MQTT user is required");
            hasError = true;
        }
        if (password.isEmpty()) {
            editTextMqttPassword.setError("MQTT password is required");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        DeviceType selectedDeviceType = (DeviceType) spinnerDeviceType.getSelectedItem();
        User selectedUser = (User) spinnerCreatorUser.getSelectedItem();

        int deviceTypeId = selectedDeviceType != null ? selectedDeviceType.id : -1;
        int creatorUserId = selectedUser != null ? selectedUser.id : -1;

        if (deviceTypeId == -1) {
            showAlert("Please select a device type");
            return;
        }

        if (creatorUserId == -1) {
            showAlert("Please select a creator user");
            return;
        }

        new Thread(() -> {
            // Check for duplicate devices with the same name and creator user
            Device existingDevice = deviceDao.getDeviceByNameAndUser(name, creatorUserId);
            if (existingDevice != null) {
                runOnUiThread(() -> {
                    showAlert("A device with the same name and creator user already exists!");
                });
            } else {
                // Insert new device
                Device newDevice = new Device(name, topic, user, password, deviceTypeId, creatorUserId);
                deviceDao.insert(newDevice);
                runOnUiThread(() -> {
                    showAlert("Device saved!");

                    Intent intent = new Intent(AddDeviceActivity.this, DeviceListActivity.class);
                    intent.putExtra("USER_ROLE", userRole);
                    setResult(RESULT_OK, intent);
                    finish();
                });
            }
        }).start();
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(AddDeviceActivity.this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}
