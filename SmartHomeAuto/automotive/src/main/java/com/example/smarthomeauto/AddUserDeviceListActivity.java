package com.example.smarthomeauto;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.room.Room;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class AddUserDeviceListActivity extends Activity {

    private static final String TAG = "AddUserDeviceListActivity";

    private ListView listViewDevices;
    private Spinner spinnerPermissions;
    private Spinner spinnerDeviceType;
    private Button buttonAddDevices;
    private SearchView searchViewDevice;
    private TextView textViewDeviceCount;
    private Device selectedDevice;

    private List<Device> availableDevices = new ArrayList<>();
    private List<Device> filteredDevices = new ArrayList<>();
    private List<Device> deviceList;
    private UserDeviceAdapter deviceAdapter;

    private DeviceDao deviceDao;
    private DeviceTypeDao deviceTypeDao;
    private UserDeviceDao userDeviceDao;
    private List<DeviceType> deviceTypeList;

    private MqttManager mqttManager;
    private View rootView;

    private int userId;
    private int guestId;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adduserdevicelist);

        rootView = findViewById(android.R.id.content);

        listViewDevices = findViewById(R.id.listViewDevices);
        spinnerPermissions = findViewById(R.id.spinnerPermissions);
        spinnerDeviceType = findViewById(R.id.spinnerDeviceType);
        buttonAddDevices = findViewById(R.id.buttonAddDevices);
        searchViewDevice = findViewById(R.id.searchViewDevice);
        textViewDeviceCount = findViewById(R.id.textViewDeviceCount);

        userId = getIntent().getIntExtra("USER_ID", -1);
        guestId = getIntent().getIntExtra("GUEST_ID", -1);
        role = getIntent().getStringExtra("USER_ROLE");
        mqttManager = new MqttManager(this, userId);

        // Initialize database DAOs
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        deviceDao = db.deviceDao();
        deviceTypeDao = db.deviceTypeDao();
        userDeviceDao = db.userDeviceDao();

        // Initialize permission spinner
        List<String> permissions = new ArrayList<>();
        permissions.add("Read");
        permissions.add("Control");
        ArrayAdapter<String> permissionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, permissions);
        permissionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPermissions.setAdapter(permissionAdapter);

        // Fetch available devices and initialize the spinner with device types
        fetchAvailableDevices();

        // Set up the back button
        findViewById(R.id.buttonBack).setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("USER_ID", userId);
            resultIntent.putExtra("USER_ROLE", role);
            resultIntent.putExtra("GUEST_ID", guestId);

            setResult(RESULT_OK, resultIntent);
            finish();
        });

        // Set up the add devices button
        buttonAddDevices.setOnClickListener(v -> addSelectedDevices());


        listViewDevices.setOnItemClickListener((parent, view, position, id) -> {
            selectedDevice = deviceAdapter.getItem(position);
            deviceAdapter.setSelectedPosition(position);
            if (selectedDevice != null) {
                Snackbar.make(findViewById(android.R.id.content), "Selected: " + selectedDevice.DeviceName, Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(findViewById(android.R.id.content), "No device selected", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAvailableDevices() {
        new Thread(() -> {
            try {
                availableDevices = deviceDao.getAvailableDevicesForUserExcludingGuest(userId, guestId);
                deviceTypeList = deviceTypeDao.getAllDeviceTypes();


                Log.d(TAG, "Number of devices fetched: " + availableDevices.size());
                for (Device device : availableDevices) {
                    Log.d(TAG, "Device ID: " + device.DevicesID + ", Device Name: " + device.DeviceName);
                }

                runOnUiThread(() -> {
                    DeviceType allTypes = new DeviceType("All", "All");
                    allTypes.DeviceTypeID = -1;
                    deviceTypeList.add(0, allTypes);

                    ArrayAdapter<DeviceType> deviceTypeAdapter = new ArrayAdapter<>(AddUserDeviceListActivity.this, android.R.layout.simple_spinner_item, deviceTypeList);
                    deviceTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerDeviceType.setAdapter(deviceTypeAdapter);

                    setupSearchView();
                    setupSpinner();

                    deviceAdapter = new UserDeviceAdapter(AddUserDeviceListActivity.this, availableDevices, deviceTypeDao);
                    listViewDevices.setAdapter(deviceAdapter);
                    updateDeviceCount(availableDevices.size());
                });
            } catch (Exception e) {
                Log.e(TAG, "Error fetching devices", e);
            }
        }).start();
    }

    private void setupSearchView() {
        searchViewDevice.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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

    private void setupSpinner() {
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
    }

    private void filterDevices() {
        String queryName = searchViewDevice.getQuery().toString().trim();
        DeviceType selectedType = (DeviceType) spinnerDeviceType.getSelectedItem();
        Integer deviceTypeId = (selectedType != null && selectedType.DeviceTypeID != -1) ? selectedType.DeviceTypeID : null;

        new Thread(() -> {
            List<Device> filteredDevices;

            filteredDevices = deviceDao.searchDevicesByCreatorIdAndFilters2(userId, queryName, deviceTypeId,guestId);


            runOnUiThread(() -> {
                deviceAdapter.clear();
                deviceAdapter.addAll(filteredDevices);
                deviceAdapter.notifyDataSetChanged();
                textViewDeviceCount.setText("Number of Devices: " + filteredDevices.size());
            });
        }).start();
    }

    private void updateDeviceCount() {
        textViewDeviceCount.setText("Number of Devices: " + filteredDevices.size());
    }

    private void addSelectedDevices() {
        List<Device> selectedDevices = new ArrayList<>();
        for (int i = 0; i < listViewDevices.getCount(); i++) {
            if (listViewDevices.isItemChecked(i)) {
                selectedDevices.add(deviceAdapter.getItem(i));
            }
        }

        String selectedPermission = spinnerPermissions.getSelectedItem().toString();

        if (selectedDevices.isEmpty()) {
            Toast.makeText(this, "Please select at least one device", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                for (Device device : selectedDevices) {
                    // Create a UserDevice entity for each selected device with the given permission
                    UserDevice userDevice = new UserDevice(guestId, device.DevicesID, selectedPermission);
                    userDeviceDao.insert(userDevice); // Insert the new UserDevice into the database
                }
                runOnUiThread(() -> {
                    Intent intent = new Intent(AddUserDeviceListActivity.this, DeviceUserListActivity.class);
                    intent.putExtra("USER_ID", userId);
                    intent.putExtra("GUEST_ID", guestId);
                    intent.putExtra("USER_ROLE", role);
                    setResult(RESULT_OK, intent);
                    finish();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error adding devices", e);
            }
        }).start();
    }

    private void updateDeviceCount(int count) {
        textViewDeviceCount.setText("Number of Devices: " + count);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttManager != null) {
            mqttManager.disconnect();
        }
    }
}
