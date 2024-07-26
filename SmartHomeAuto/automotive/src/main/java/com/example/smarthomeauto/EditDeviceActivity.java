package com.example.smarthomeauto;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.room.Room;

import java.util.List;

public class EditDeviceActivity extends Activity {

    private EditText editTextDeviceName;
    private EditText editTextMqttTopic;
    private EditText editTextMqttServer;
    private EditText editTextMqttUser;
    private EditText editTextMqttPassword;
    private EditText editTextMqttPort;
    private Spinner spinnerDeviceType;
    private Button buttonSaveDevice;
    private Button buttonBack; // Declare o botão "Back"
    private Device device;
    private List<DeviceType> deviceTypeList;
    private DeviceDao deviceDao;
    private DeviceTypeDao deviceTypeDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

        TextView formTitle = findViewById(R.id.formTitle);
        editTextDeviceName = findViewById(R.id.editTextDeviceName);
        editTextMqttTopic = findViewById(R.id.editTextMqttTopic);
        editTextMqttServer = findViewById(R.id.editTextMqttServer);
        editTextMqttUser = findViewById(R.id.editTextMqttUser);
        editTextMqttPassword = findViewById(R.id.editTextMqttPassword);
        editTextMqttPort = findViewById(R.id.editTextMqttPort);
        spinnerDeviceType = findViewById(R.id.spinnerDeviceType);
        buttonSaveDevice = findViewById(R.id.buttonSaveDevice);
        buttonBack = findViewById(R.id.buttonBack); // Inicialize o botão "Back" aqui

        AppDatabase database = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        deviceDao = database.deviceDao();
        deviceTypeDao = database.deviceTypeDao();

        Intent intent = getIntent();
        if (intent.hasExtra("device")) {
            device = (Device) intent.getSerializableExtra("device");
            formTitle.setText("Edit Device");
            editTextDeviceName.setText(device.name);
            editTextMqttTopic.setText(device.mqttTopic);
            editTextMqttServer.setText(device.mqttServer);
            editTextMqttUser.setText(device.mqttUser);
            editTextMqttPassword.setText(device.mqttPassword);
            editTextMqttPort.setText(String.valueOf(device.mqttPort));
        } else {
            formTitle.setText("Add New Device");
        }

        // Carregue os tipos de dispositivos
        loadDeviceTypes();

        // Configurar o botão de salvar
        buttonSaveDevice.setOnClickListener(v -> saveDevice());

        // Configurar o botão de voltar
        buttonBack.setOnClickListener(v -> {
            // Cria um Intent para iniciar a DeviceListActivity
            Intent intent2 = new Intent(EditDeviceActivity.this, DeviceListActivity.class);
            startActivity(intent2);
            finish(); // Finaliza a EditDeviceActivity para removê-la da pilha de atividades
        });
    }

    private void loadDeviceTypes() {
        new Thread(() -> {
            deviceTypeList = deviceTypeDao.getAllDeviceTypes(); // Recupera todos os tipos de dispositivos
            runOnUiThread(() -> {
                ArrayAdapter<DeviceType> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deviceTypeList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDeviceType.setAdapter(adapter);

                // Selecionar o tipo de dispositivo correto, se estiver editando
                if (device != null) {
                    setSpinnerSelection(device.deviceTypeId);
                }
            });
        }).start();
    }

    private void setSpinnerSelection(int deviceTypeId) {
        for (int i = 0; i < spinnerDeviceType.getCount(); i++) {
            DeviceType type = (DeviceType) spinnerDeviceType.getItemAtPosition(i);
            if (type.id == deviceTypeId) {
                spinnerDeviceType.setSelection(i);
                break;
            }
        }
    }

    private void saveDevice() {
        String name = editTextDeviceName.getText().toString().trim();
        String topic = editTextMqttTopic.getText().toString().trim();
        String server = editTextMqttServer.getText().toString().trim();
        String user = editTextMqttUser.getText().toString().trim();
        String password = editTextMqttPassword.getText().toString().trim();
        String portString = editTextMqttPort.getText().toString().trim();
        DeviceType selectedDeviceType = (DeviceType) spinnerDeviceType.getSelectedItem();

        // Verifique se o campo de porta é um número válido
        int port;
        try {
            port = Integer.parseInt(portString);
        } catch (NumberFormatException e) {
            editTextMqttPort.setError("Invalid port number");
            return;
        }

        int deviceTypeId = selectedDeviceType.id;

        if (name.isEmpty() || topic.isEmpty() || server.isEmpty() || user.isEmpty() || password.isEmpty()) {
            // Exibe uma mensagem de erro
            if (name.isEmpty()) editTextDeviceName.setError("Device name is required");
            if (topic.isEmpty()) editTextMqttTopic.setError("MQTT topic is required");
            if (server.isEmpty()) editTextMqttServer.setError("MQTT server is required");
            if (user.isEmpty()) editTextMqttUser.setError("MQTT user is required");
            if (password.isEmpty()) editTextMqttPassword.setError("MQTT password is required");
            return;
        }

        new Thread(() -> {
            if (device != null) {
                device.name = name;
                device.mqttTopic = topic;
                device.mqttServer = server;
                device.mqttUser = user;
                device.mqttPassword = password;
                device.mqttPort = port;
                device.deviceTypeId = deviceTypeId;

                // Atualiza o dispositivo na base de dados
                deviceDao.update(device);
            } else {
                device = new Device(name, topic, server, user, password, port, deviceTypeId);
                // Insere um novo dispositivo na base de dados
                deviceDao.insert(device);
            }

            // Cria um Intent para retornar o dispositivo atualizado para a Activity anterior
            Intent resultIntent = new Intent();
            resultIntent.putExtra("device", device);
            setResult(RESULT_OK, resultIntent);
            finish();
        }).start();
    }
}
