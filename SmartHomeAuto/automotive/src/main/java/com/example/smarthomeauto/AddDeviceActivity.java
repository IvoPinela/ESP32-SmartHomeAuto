package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import java.util.List;

public class AddDeviceActivity extends AppCompatActivity {

    private EditText editTextDeviceName, editTextMqttTopic, editTextMqttServer, editTextMqttUser, editTextMqttPassword, editTextMqttPort;
    private Spinner spinnerDeviceType;
    private Button buttonSaveDevice;
    private DeviceDao deviceDao;
    private Button buttonBack;
    private DeviceTypeDao deviceTypeDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);


        editTextDeviceName = findViewById(R.id.editTextDeviceName);
        editTextMqttTopic = findViewById(R.id.editTextMqttTopic);
        editTextMqttServer = findViewById(R.id.editTextMqttServer);
        editTextMqttUser = findViewById(R.id.editTextMqttUser);
        editTextMqttPassword = findViewById(R.id.editTextMqttPassword);
        editTextMqttPort = findViewById(R.id.editTextMqttPort);
        spinnerDeviceType = findViewById(R.id.spinnerDeviceType);
        buttonSaveDevice = findViewById(R.id.buttonSaveDevice);
        Button buttonBack = findViewById(R.id.buttonBack);


        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        deviceDao = db.deviceDao();
        deviceTypeDao = db.deviceTypeDao();


        loadDeviceTypes();

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(AddDeviceActivity.this, DeviceListActivity.class);
            startActivity(intent);
            finish(); // Finaliza a AddDeviceActivity para removê-la da pilha de atividades
        });

        buttonBack.setOnClickListener(v -> {
            finish();
        });

        buttonSaveDevice.setOnClickListener(v -> {
            String deviceName = editTextDeviceName.getText().toString();
            String mqttTopic = editTextMqttTopic.getText().toString();
            String mqttServer = editTextMqttServer.getText().toString();
            String mqttUser = editTextMqttUser.getText().toString();
            String mqttPassword = editTextMqttPassword.getText().toString();
            String mqttPortString = editTextMqttPort.getText().toString();

            if (deviceName.isEmpty() || mqttTopic.isEmpty() || mqttServer.isEmpty() || mqttUser.isEmpty() ||
                    mqttPassword.isEmpty() || mqttPortString.isEmpty()) {
                showAlert("All fields must be filled");
                return;
            }

            int mqttPort;
            try {
                mqttPort = Integer.parseInt(mqttPortString);
            } catch (NumberFormatException e) {
                showAlert("Invalid MQTT Port");
                return;
            }

            DeviceType selectedDeviceType = (DeviceType) spinnerDeviceType.getSelectedItem();
            int deviceTypeId = selectedDeviceType != null ? selectedDeviceType.id : -1;

            if (deviceTypeId == -1) {
                showAlert("Please select a device type");
                return;
            }

            Device newDevice = new Device(deviceName, mqttTopic, mqttServer, mqttUser, mqttPassword, mqttPort, deviceTypeId);

            new Thread(() -> {
                deviceDao.insert(newDevice);
                runOnUiThread(() -> {
                    showAlert("Device saved!");
                    setResult(RESULT_OK);
                    finish();
                });
            }).start();
        });


    }

    private void loadDeviceTypes() {
        new Thread(() -> {
            List<DeviceType> deviceTypes = deviceTypeDao.getAllDeviceTypes();
            runOnUiThread(() -> {
                DeviceTypeAdapter adapter = new DeviceTypeAdapter(AddDeviceActivity.this, deviceTypes);
                spinnerDeviceType.setAdapter(adapter);
            });
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
