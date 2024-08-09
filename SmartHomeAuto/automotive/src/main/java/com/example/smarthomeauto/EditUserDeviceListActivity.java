package com.example.smarthomeauto;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import com.google.android.material.snackbar.Snackbar;

public class EditUserDeviceListActivity extends AppCompatActivity {

    private TextView textViewDeviceName;
    private TextView textViewDeviceType;
    private Spinner spinnerPermissions;
    private Button buttonSave;
    private Button buttonCancel;

    private int deviceId;
    private int userId;
    private int guestId;
    private String role;

    private UserDeviceDao userDeviceDao;
    private DeviceDao deviceDao;
    private String[] permissions = {"read", "control"};
    private String currentPermission;
    private View rootView;
    private MqttManager mqttManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edituserdevice);

        rootView = findViewById(android.R.id.content);

        // Initialize views
        textViewDeviceName = findViewById(R.id.textViewDeviceName);
        textViewDeviceType = findViewById(R.id.textViewDeviceType);
        spinnerPermissions = findViewById(R.id.spinnerPermissions);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);

        // Get device data from Intent
        Intent intent = getIntent();
        deviceId = intent.getIntExtra("DEVICE_ID", -1);
        String deviceName = intent.getStringExtra("DEVICE_NAME");
        String deviceType = intent.getStringExtra("DEVICE_TYPE");
        currentPermission = intent.getStringExtra("DEVICE_PERMISSION");

        userId=intent.getIntExtra("USER_ID",-1 );
        guestId=intent.getIntExtra("GUEST_ID",-1);
        role=intent.getStringExtra("USER_ROLE");

        mqttManager = new MqttManager(this, userId);
        // Set device information
        textViewDeviceName.setText(deviceName);
        textViewDeviceType.setText(deviceType);

        // Set up spinner with permissions
        ArrayAdapter<String> permissionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, permissions);
        permissionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPermissions.setAdapter(permissionsAdapter);

        // Set current permission in spinner
        int permissionPosition = ((ArrayAdapter<String>) spinnerPermissions.getAdapter()).getPosition(currentPermission);
        spinnerPermissions.setSelection(permissionPosition);

        buttonSave.setOnClickListener(v -> {
            saveChanges();

            Intent returnIntent = new Intent(EditUserDeviceListActivity.this, DeviceUserListActivity.class);
            returnIntent.putExtra("USER_ID", userId);
            returnIntent.putExtra("GUEST_ID", guestId);
            returnIntent.putExtra("USER_ROLE", role);
            startActivity(returnIntent);
            finish();
        });

        buttonCancel.setOnClickListener(v -> {

            Intent returnIntent = new Intent(EditUserDeviceListActivity.this, DeviceUserListActivity.class);
            returnIntent.putExtra("USER_ID", userId);
            returnIntent.putExtra("GUEST_ID", guestId);
            returnIntent.putExtra("USER_ROLE", role);

            returnIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(returnIntent);
            finish();
        });
    }

    private void saveChanges() {
        String selectedPermission = (String) spinnerPermissions.getSelectedItem();

        // Update permission in the database
        new Thread(() -> {
            AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
            userDeviceDao = db.userDeviceDao();
            UserDevice userDevice = userDeviceDao.getUserDevice(guestId,deviceId);

            if (userDevice != null) {
                userDevice.permissions = selectedPermission;
                userDeviceDao.update(userDevice);

                runOnUiThread(() -> {
                    // Notify user and finish activity
                    Snackbar.make(findViewById(android.R.id.content), "Permission updated", Snackbar.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttManager != null) {
            mqttManager.disconnect();
        }
    }
}
