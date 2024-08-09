package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import java.util.List;

public class AddDevicesUserListActivity extends AppCompatActivity {

    private EditText editTextDeviceName;
    private Spinner spinnerDeviceType;
    private Button buttonSaveDevice, buttonBack;
    private DeviceDao deviceDao;
    private DeviceTypeDao deviceTypeDao;
    private UserDao userDao;
    private String userRole;
    private int creatorUserId;
    private MqttManager mqttManager;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_userdevice);

        rootView = findViewById(android.R.id.content);

        Intent intent2 = getIntent();
        if (intent2 != null) {
            userRole = intent2.getStringExtra("USER_ROLE");
            creatorUserId = intent2.getIntExtra("USER_ID", -1);
            Log.d("UserListActivity", "User Role: " + userRole);
        }

        mqttManager = new MqttManager(this, creatorUserId);
        editTextDeviceName = findViewById(R.id.editTextDeviceName);
        spinnerDeviceType = findViewById(R.id.spinnerDeviceType);
        buttonSaveDevice = findViewById(R.id.buttonSaveDevice);
        buttonBack = findViewById(R.id.buttonBack);

        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        deviceDao = db.deviceDao();
        deviceTypeDao = db.deviceTypeDao();
        userDao = db.userDao();

        loadDeviceTypes();

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(AddDevicesUserListActivity.this, DevicesUserListActivity.class);
            intent.putExtra("USER_ROLE", userRole);
            intent.putExtra("USER_ID", creatorUserId);
            startActivity(intent);
            finish();
        });

        buttonSaveDevice.setOnClickListener(v -> saveDevice());
    }

    private void loadDeviceTypes() {
        new Thread(() -> {
            List<DeviceType> deviceTypes = deviceTypeDao.getAllDeviceTypes();
            runOnUiThread(() -> {
                ArrayAdapter<DeviceType> adapter = new ArrayAdapter<>(AddDevicesUserListActivity.this, android.R.layout.simple_spinner_item, deviceTypes);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDeviceType.setAdapter(adapter);
            });
        }).start();
    }

    private void saveDevice() {
        boolean hasError = false;
        String name = editTextDeviceName.getText().toString().trim();

        // Validate fields
        if (name.isEmpty()) {
            editTextDeviceName.setError("Device name is required");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        DeviceType selectedDeviceType = (DeviceType) spinnerDeviceType.getSelectedItem();

        int deviceTypeId = selectedDeviceType != null ? selectedDeviceType.DeviceTypeID : -1;

        if (deviceTypeId == -1) {
            showAlert("Please select a device type");
            return;
        }

        // Generate topic using mqttPrincipalTopic and device name
        String topic = selectedDeviceType.MqttPrincipalTopic + "/" + name.toLowerCase().replace(" ", "");

        new Thread(() -> {
            // Check for duplicate devices with the same name and creator user
            Device existingDevice = deviceDao.getDeviceByNameAndUser(name, creatorUserId);
            if (existingDevice != null) {
                runOnUiThread(() -> {
                    showAlert("A device with the same name and creator user already exists!");
                });
            } else {
                // Insert new device
                Device newDevice = new Device(name, topic, null, null, deviceTypeId, creatorUserId);
                deviceDao.insert(newDevice);
                runOnUiThread(() -> {
                    showAlert("Device saved!");

                    Intent intent = new Intent(AddDevicesUserListActivity.this, DeviceListActivity.class);
                    intent.putExtra("USER_ROLE", userRole);
                    intent.putExtra("USER_ID", creatorUserId);
                    setResult(RESULT_OK, intent);
                    finish();
                });
            }
        }).start();
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(AddDevicesUserListActivity.this)
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
