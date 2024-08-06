package com.example.smarthomeauto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.room.Room;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class DevicesUserListActivity extends AppCompatActivity {

    private static final int REQUEST_ADD_DEVICE = 1;
    private static final int REQUEST_EDIT_DEVICE = 2;

    private ListView listViewDevices;
    private UserDeviceAdapter userDeviceAdapter;
    private List<Device> deviceList;
    private DeviceDao deviceDao;
    private DeviceTypeDao deviceTypeDao;
    private List<DeviceType> deviceTypeList;
    private Device selectedDevice;
    private Spinner spinnerDeviceType;
    private SearchView searchViewName;
    private TextView textViewDeviceCount;
    private int creatorUserId;
    private String userrole;
    private MqttManager mqttManager;
    private View rootView;
    private static final String TAG = "Device List";
    private static final String LIGHT_CHANNEL_ID = "light_status_channel";
    private static final String GATE_CHANNEL_ID = "gate_status_channel";


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.deviceuserlist);


        rootView = findViewById(android.R.id.content);

        Button buttonBack = findViewById(R.id.buttonBack);
        Button buttonAddDevice = findViewById(R.id.buttonAddDevice);
        ImageButton buttonDelete = findViewById(R.id.buttonDelete);
        ImageButton buttonEditDevice = findViewById(R.id.buttonEdit);
        spinnerDeviceType = findViewById(R.id.spinnerDeviceType);
        searchViewName = findViewById(R.id.searchViewName);
        listViewDevices = findViewById(R.id.listViewDevices);
        textViewDeviceCount = findViewById(R.id.textViewDeviceCount);

        creatorUserId = getIntent().getIntExtra("USER_ID", -1);
        userrole=getIntent().getStringExtra("USER_ROLE");
        deviceDao = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build().deviceDao();
        deviceTypeDao = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build().deviceTypeDao();

        mqttManager = new MqttManager(this, creatorUserId);

        buttonBack.setOnClickListener(v -> finish());

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("USER_ID", creatorUserId);
            intent.putExtra("USER_ROLE", userrole);
            setResult(RESULT_OK, intent);
            finish();
        });
        buttonAddDevice.setOnClickListener(v -> {
            Intent intent = new Intent(DevicesUserListActivity.this, AddDevicesUserListActivity.class);
            intent.putExtra("USER_ID", creatorUserId);
            intent.putExtra("USER_ROLE", userrole);
            startActivityForResult(intent, REQUEST_ADD_DEVICE);
        });


        buttonEditDevice.setOnClickListener(v -> {
            if (selectedDevice != null) {
                Intent intent = new Intent(DevicesUserListActivity.this, EditDevicesUserListActivity.class);
                intent.putExtra("DEVICE", selectedDevice);
                intent.putExtra("USER_ID", creatorUserId);
                intent.putExtra("USER_ROLE", userrole);
                startActivityForResult(intent, REQUEST_EDIT_DEVICE);
            } else {
                Snackbar.make(findViewById(android.R.id.content), "No device selected", Snackbar.LENGTH_SHORT).show();
            }
        });

        buttonDelete.setOnClickListener(v -> {
            if (selectedDevice != null) {
                showDeleteConfirmationDialog();
            } else {
                Snackbar.make(findViewById(android.R.id.content), "No device selected", Snackbar.LENGTH_SHORT).show();
            }
        });

        listViewDevices.setOnItemClickListener((parent, view, position, id) -> {
            selectedDevice = deviceList.get(position);
            userDeviceAdapter.setSelectedPosition(position);
            Snackbar.make(findViewById(android.R.id.content), "Selected: " + selectedDevice.name, Snackbar.LENGTH_SHORT).show();
        });

        setupSpinner();
        setupSearchViews();
        loadDevices();
    }

    private void setupSpinner() {
        new Thread(() -> {
            deviceTypeList = deviceTypeDao.getAllDeviceTypes();
            runOnUiThread(() -> {
                DeviceType allTypes = new DeviceType("All", "All");
                allTypes.id = -1;
                deviceTypeList.add(0, allTypes);

                ArrayAdapter<DeviceType> spinnerAdapter = new ArrayAdapter<>(DevicesUserListActivity.this, android.R.layout.simple_spinner_item, deviceTypeList);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDeviceType.setAdapter(spinnerAdapter);

                spinnerDeviceType.setSelection(0);

                spinnerDeviceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        filterDevices();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        filterDevices();
                    }
                });
            });
        }).start();
    }

    private void setupSearchViews() {
        searchViewName.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterDevices();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterDevices();
                return true;
            }
        });
    }

    private void loadDevices() {
        new Thread(() -> {

            deviceList = deviceDao.getDevicesByUserId(creatorUserId);
            runOnUiThread(() -> {
                userDeviceAdapter = new UserDeviceAdapter(DevicesUserListActivity.this, deviceList, deviceTypeDao);
                listViewDevices.setAdapter(userDeviceAdapter);
                textViewDeviceCount.setText("Number of Devices: " + deviceList.size());
            });
        }).start();
    }

    private void filterDevices() {
        String queryName = searchViewName.getQuery().toString().trim();
        DeviceType selectedType = (DeviceType) spinnerDeviceType.getSelectedItem();
        Integer deviceTypeId = (selectedType != null && selectedType.id != -1) ? selectedType.id : null;

        new Thread(() -> {
            List<Device> filteredDevices;

            filteredDevices = deviceDao.searchDevicesByCreatorIdAndFilters(creatorUserId, queryName, deviceTypeId);


            runOnUiThread(() -> {
                userDeviceAdapter.clear();
                userDeviceAdapter.addAll(filteredDevices);
                userDeviceAdapter.notifyDataSetChanged();
                textViewDeviceCount.setText("Number of Devices: " + filteredDevices.size());
            });
        }).start();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Device")
                .setMessage("Are you sure you want to delete this device?")
                .setPositiveButton("Delete", (dialog, which) -> deleteDevice())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDevice() {
        if (selectedDevice != null) {
            new Thread(() -> {
                deviceDao.delete(selectedDevice);
                runOnUiThread(() -> {
                    Snackbar.make(findViewById(android.R.id.content), "Device deleted", Snackbar.LENGTH_SHORT).show();
                    loadDevices();
                    selectedDevice = null;
                });
            }).start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_DEVICE || requestCode == REQUEST_EDIT_DEVICE) {
            if (resultCode == RESULT_OK) {
                loadDevices();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttManager != null) {
            mqttManager.disconnect();
        }
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Light Status Channel
            CharSequence lightName = "Light Status";
            String lightDescription = "Channel for light status notifications";
            int lightImportance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel lightChannel = new NotificationChannel(LIGHT_CHANNEL_ID, lightName, lightImportance);
            lightChannel.setDescription(lightDescription);

            // Gate Status Channel
            CharSequence gateName = "Gate Status";
            String gateDescription = "Channel for gate status notifications";
            int gateImportance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel gateChannel = new NotificationChannel(GATE_CHANNEL_ID, gateName, gateImportance);
            gateChannel.setDescription(gateDescription);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(lightChannel);
                notificationManager.createNotificationChannel(gateChannel);
                Log.i(TAG, "Notification channels created.");
            } else {
                Log.e(TAG, "Failed to create notification channels.");
            }
        }
    }
}
