package com.example.smarthomeauto;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;

public class EditDeviceActivity extends Activity {

    private EditText editTextDeviceName;
    private EditText editTextMqttTopic;
    private EditText editTextMqttUser;
    private EditText editTextMqttPassword;
    private Spinner spinnerDeviceType;
    private Spinner spinnerCreatorUser; // Spinner for Creator User
    private Button buttonSaveDevice;
    private Button buttonBack; // Declare o botão "Back"
    private Device device;
    private List<DeviceType> deviceTypeList;
    private List<User> userList; // List of users for the Creator User spinner
    private DeviceDao deviceDao;
    private DeviceTypeDao deviceTypeDao;
    private UserDao userDao;// DAO for User
    private String userRole;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        Intent intent3 = getIntent();
        if (intent3 != null) {
            userRole = intent3.getStringExtra("USER_ROLE");
            Log.d("UserListActivity", "User Role: " + userRole);
        }
        TextView formTitle = findViewById(R.id.formTitle);
        editTextDeviceName = findViewById(R.id.editTextDeviceName);
        editTextMqttTopic = findViewById(R.id.editTextMqttTopic);
        editTextMqttUser = findViewById(R.id.editTextMqttUser);
        editTextMqttPassword = findViewById(R.id.editTextMqttPassword);
        spinnerDeviceType = findViewById(R.id.spinnerDeviceType);
        spinnerCreatorUser = findViewById(R.id.spinnerCreatorUser); // Initialize the Creator User spinner
        buttonSaveDevice = findViewById(R.id.buttonSaveDevice);
        buttonBack = findViewById(R.id.buttonBack); // Initialize the Back button

        AppDatabase database = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        deviceDao = database.deviceDao();
        deviceTypeDao = database.deviceTypeDao();
        userDao = database.userDao(); // Initialize User DAO

        Intent intent = getIntent();
        if (intent.hasExtra("device")) {
            device = (Device) intent.getSerializableExtra("device");
            formTitle.setText("Edit Device");
            editTextDeviceName.setText(device.DeviceName);
            editTextMqttTopic.setText(device.MqttSubTopic);
            editTextMqttUser.setText(device.MqttUser);
            editTextMqttPassword.setText(device.MqttPassword);
        } else {
            formTitle.setText("Add New Device");
        }

        loadDeviceTypes();
        loadUsers();

        buttonSaveDevice.setOnClickListener(v -> saveDevice());

        buttonBack.setOnClickListener(v -> {
            Intent intent2 = new Intent(EditDeviceActivity.this, DeviceListActivity.class);
            intent2.putExtra("USER_ROLE", userRole);
            startActivity(intent2);
            finish();
        });
    }

    private void loadDeviceTypes() {
        new Thread(() -> {
            deviceTypeList = deviceTypeDao.getAllDeviceTypes();
            runOnUiThread(() -> {
                ArrayAdapter<DeviceType> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deviceTypeList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDeviceType.setAdapter(adapter);

                if (device != null) {
                    setSpinnerSelection(device.TypeId);
                }
            });
        }).start();
    }

    private void loadUsers() {
        new Thread(() -> {
            userList = userDao.getAllUsers(); // Load all users
            runOnUiThread(() -> {
                List<User> filteredUsers = new ArrayList<>();
                for (User user : userList) {
                    if ("user".equals(user.Role)) {
                        filteredUsers.add(user);
                    }
                }
                ArrayAdapter<User> adapter = new ArrayAdapter<>(EditDeviceActivity.this, android.R.layout.simple_spinner_item, filteredUsers);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCreatorUser.setAdapter(adapter);

                if (device != null) {
                    setSpinnerCreatorUserSelection(device.CreatorUserId);
                }
            });
        }).start();
    }

    private void setSpinnerSelection(int deviceTypeId) {
        for (int i = 0; i < spinnerDeviceType.getCount(); i++) {
            DeviceType type = (DeviceType) spinnerDeviceType.getItemAtPosition(i);
            if (type.DeviceTypeID == deviceTypeId) {
                spinnerDeviceType.setSelection(i);
                break;
            }
        }
    }

    private void setSpinnerCreatorUserSelection(int creatorUserId) {
        for (int i = 0; i < spinnerCreatorUser.getCount(); i++) {
            User user = (User) spinnerCreatorUser.getItemAtPosition(i);
            if (user.UserID == creatorUserId) {
                spinnerCreatorUser.setSelection(i);
                break;
            }
        }
    }

    private void saveDevice() {
        String name = editTextDeviceName.getText().toString().trim();
        String topic = editTextMqttTopic.getText().toString().trim();
        String user = editTextMqttUser.getText().toString().trim();
        String password = editTextMqttPassword.getText().toString().trim();
        DeviceType selectedDeviceType = (DeviceType) spinnerDeviceType.getSelectedItem();
        User selectedCreatorUser = (User) spinnerCreatorUser.getSelectedItem(); // Get selected creator user

        // Flag to track if there are validation errors
        boolean hasError = false;

        // Clear previous errors
        editTextDeviceName.setError(null);
        editTextMqttTopic.setError(null);
        editTextMqttUser.setError(null);
        editTextMqttPassword.setError(null);

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

        // Ensure a Device Type and Creator User are selected
        if (selectedDeviceType == null) {
            // Handle case where Device Type is not selected
            showAlert("Please select a device type");
            hasError = true;
        }
        if (selectedCreatorUser == null) {
            // Handle case where Creator User is not selected
            showAlert("Please select a creator user");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        new Thread(() -> {
            int deviceTypeId = selectedDeviceType.DeviceTypeID;
            int creatorUserId = selectedCreatorUser.UserID; // Get creator user ID

            Device existingDevice = deviceDao.getDeviceByNameAndUserExceptId(name, creatorUserId, device != null ? device.DevicesID : -1);
            if (existingDevice != null) {
                runOnUiThread(() -> {
                    showAlert("A device with the same name and creator user already exists!");
                });
            } else {
                if (device != null) {
                    device.DeviceName = name;
                    device.MqttSubTopic = topic;
                    device.MqttUser = user;
                    device.MqttPassword = password;
                    device.TypeId = deviceTypeId;
                    device.CreatorUserId = creatorUserId; // Update creator user ID

                    // Update device in the database
                    deviceDao.update(device);
                } else {
                    device = new Device(name, topic, user, password, deviceTypeId, creatorUserId);
                    deviceDao.insert(device);
                }

                Intent resultIntent = new Intent();
                resultIntent.putExtra("device", device);
                resultIntent.putExtra("USER_ROLE", userRole);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        }).start();
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(EditDeviceActivity.this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}
