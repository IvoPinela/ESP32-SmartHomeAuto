package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;


import java.util.List;

public class EditDevicesUserListActivity extends AppCompatActivity {

    private EditText editTextDeviceName;
    private Spinner spinnerDeviceType;
    private Button buttonSaveDevice, buttonBack;
    private DeviceDao deviceDao;
    private DeviceTypeDao deviceTypeDao;
    private UserDao userDao;
    private String userRole;
    private int creatorUserId;
    private int deviceId;
    private MqttManager mqttManager;
    private View rootView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_userdevice);

        rootView = findViewById(android.R.id.content);
        TextView titleForm = findViewById(R.id.formTitle);
        titleForm.setText("Edit Devices");

        Intent intent = getIntent();
        if (intent != null) {
            userRole = intent.getStringExtra("USER_ROLE");
            creatorUserId = intent.getIntExtra("USER_ID", -1);
            Device device = (Device) intent.getSerializableExtra("DEVICE");
            mqttManager = new MqttManager(this, creatorUserId);

            if (device != null) {
                deviceId = device.id;
                // Initialize views and load device data
                editTextDeviceName = findViewById(R.id.editTextDeviceName);
                spinnerDeviceType = findViewById(R.id.spinnerDeviceType);
                buttonSaveDevice = findViewById(R.id.buttonSaveDevice);
                buttonBack = findViewById(R.id.buttonBack);

                AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
                deviceDao = db.deviceDao();
                deviceTypeDao = db.deviceTypeDao();
                userDao = db.userDao();

                loadDeviceTypes();
                loadDeviceData(device);

                buttonSaveDevice.setOnClickListener(v -> saveDevice());
                buttonBack.setOnClickListener(v -> {
                    Intent backIntent = new Intent(EditDevicesUserListActivity.this, DevicesUserListActivity.class);
                    backIntent.putExtra("USER_ROLE", userRole);
                    backIntent.putExtra("USER_ID", creatorUserId);
                    startActivity(backIntent);
                    finish();
                });
            } else {
                Log.e("EditDeviceActivity", "Device not found");
                finish(); // Exit if device is not found
            }
        }
    }

    private void loadDeviceTypes() {
        new Thread(() -> {
            List<DeviceType> deviceTypes = deviceTypeDao.getAllDeviceTypes();
            runOnUiThread(() -> {
                ArrayAdapter<DeviceType> adapter = new ArrayAdapter<>(EditDevicesUserListActivity.this, android.R.layout.simple_spinner_item, deviceTypes);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDeviceType.setAdapter(adapter);
            });
        }).start();
    }

    private void loadDeviceData(Device device) {
        // Initialize views and populate data
        editTextDeviceName.setText(device.name);
        new Thread(() -> {
            List<DeviceType> deviceTypes = deviceTypeDao.getAllDeviceTypes();
            runOnUiThread(() -> {
                ArrayAdapter<DeviceType> adapter = new ArrayAdapter<>(EditDevicesUserListActivity.this, android.R.layout.simple_spinner_item, deviceTypes);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDeviceType.setAdapter(adapter);
                for (int i = 0; i < spinnerDeviceType.getCount(); i++) {
                    DeviceType deviceType = (DeviceType) spinnerDeviceType.getItemAtPosition(i);
                    if (deviceType.id == device.deviceTypeId) {
                        spinnerDeviceType.setSelection(i);
                        break;
                    }
                }
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
        int deviceTypeId = selectedDeviceType != null ? selectedDeviceType.id : -1;

        if (deviceTypeId == -1) {
            showAlert("Please select a device type");
            return;
        }

        // Generate topic using mqttPrincipalTopic and device name
        String topic = selectedDeviceType.mqttPrincipalTopic + "/" + name.toLowerCase().replace(" ", "");

        new Thread(() -> {
            Device existingDevice = deviceDao.getDeviceByNameAndUser(name, creatorUserId);
            if (existingDevice != null && existingDevice.id != deviceId) {
                runOnUiThread(() -> {
                    showAlert("A device with the same name and creator user already exists!");
                });
            } else {
                Device updatedDevice = new Device(name, topic, null, null, deviceTypeId, creatorUserId);
                updatedDevice.id = deviceId;  // Ensure ID is set for update
                deviceDao.update(updatedDevice);
                runOnUiThread(() -> {
                    showAlert("Device updated!");

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("USER_ROLE", userRole);
                    resultIntent.putExtra("USER_ID", creatorUserId);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
            }
        }).start();
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(EditDevicesUserListActivity.this)
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
