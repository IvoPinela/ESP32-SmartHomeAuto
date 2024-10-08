package com.example.smarthomeauto;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import androidx.room.Room;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class DeviceListActivity extends AppCompatActivity {

    private static final int REQUEST_ADD_DEVICE = 1;
    private static final int REQUEST_EDIT_DEVICE = 2;

    private ListView listViewDevices;
    private DeviceAdapter deviceAdapter;
    private List<Device> deviceList;
    private DeviceDao deviceDao;
    private DeviceTypeDao deviceTypeDao;
    private List<DeviceType> deviceTypeList;
    private Device selectedDevice;
    private Spinner spinnerDeviceType;
    private SearchView searchViewName;
    private SearchView searchViewUser;
    private TextView textViewDeviceCount;
    private Switch switchFilterMissingMQTT;
    private int creatorUserId;
    private String userrole;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.devicelist);

        Button buttonBack = findViewById(R.id.buttonBack);
        Button buttonAddDevice = findViewById(R.id.buttonAddDevice);
        ImageButton buttonDelete = findViewById(R.id.buttonDelete);
        ImageButton buttonEditDevice = findViewById(R.id.buttonEdit);
        spinnerDeviceType = findViewById(R.id.spinnerDeviceType);
        searchViewName = findViewById(R.id.searchViewName);
        searchViewUser = findViewById(R.id.searchViewUser);
        listViewDevices = findViewById(R.id.listViewDevices);
        textViewDeviceCount = findViewById(R.id.textViewDeviceCount);
        switchFilterMissingMQTT = findViewById(R.id.switchFilterNullFields);

        deviceDao = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build().deviceDao();
        deviceTypeDao = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build().deviceTypeDao();

        userrole=getIntent().getStringExtra("USER_ROLE");

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("USER_ROLE", userrole);
            setResult(RESULT_OK, intent);
            finish();
        });

        buttonAddDevice.setOnClickListener(v -> {
            Intent intent = new Intent(DeviceListActivity.this, AddDeviceActivity.class);
            userrole=getIntent().getStringExtra("USER_ROLE");
            startActivityForResult(intent, REQUEST_ADD_DEVICE);
        });

        buttonEditDevice.setOnClickListener(v -> {
            if (selectedDevice != null) {
                Intent intent = new Intent(DeviceListActivity.this, EditDeviceActivity.class);
                userrole=getIntent().getStringExtra("USER_ROLE");
                intent.putExtra("device", selectedDevice);
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
            deviceAdapter.setSelectedPosition(position);
            Snackbar.make(findViewById(android.R.id.content), "Selected: " + selectedDevice.DeviceName, Snackbar.LENGTH_SHORT).show();
        });

        switchFilterMissingMQTT.setOnCheckedChangeListener((buttonView, isChecked) -> filterDevices());

        setupSpinner();
        setupSearchViews();
        loadDevices();
    }

    private void setupSpinner() {
        new Thread(() -> {
            deviceTypeList = deviceTypeDao.getAllDeviceTypes();
            runOnUiThread(() -> {
                DeviceType allTypes = new DeviceType("All", "All");
                allTypes.DeviceTypeID = -1;
                deviceTypeList.add(0, allTypes);

                ArrayAdapter<DeviceType> spinnerAdapter = new ArrayAdapter<>(DeviceListActivity.this, android.R.layout.simple_spinner_item, deviceTypeList);
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

        searchViewUser.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
            deviceList = deviceDao.getAllDevices();
            runOnUiThread(() -> {
                deviceAdapter = new DeviceAdapter(DeviceListActivity.this, deviceList);
                listViewDevices.setAdapter(deviceAdapter);
                textViewDeviceCount.setText("Number of Devices: " + deviceList.size());
            });
        }).start();
    }
/*
    private void filterDevices() {
        String queryName = searchViewName.getQuery().toString().trim();
        String queryUser = searchViewUser.getQuery().toString().trim();
        DeviceType selectedType = (DeviceType) spinnerDeviceType.getSelectedItem();
        Integer deviceTypeId = (selectedType != null && selectedType.DeviceTypeID != -1) ? selectedType.DeviceTypeID : null;

        boolean filterMissingMQTT = switchFilterMissingMQTT.isChecked();

        new Thread(() -> {
            List<Device> filteredDevices;

            filteredDevices = deviceDao.searchDevices(queryName, deviceTypeId, queryUser);


            if (filterMissingMQTT) {
                filteredDevices.removeIf(device -> device.MqttUser != null && !device.MqttUser.isEmpty() &&
                        device.MqttPassword != null && !device.MqttPassword.isEmpty());
            }

            runOnUiThread(() -> {
                deviceAdapter.clear();
                deviceAdapter.addAll(filteredDevices);
                deviceAdapter.notifyDataSetChanged();
                textViewDeviceCount.setText("Number of Devices: " + filteredDevices.size());
            });
        }).start();
    }*/

    private void filterDevices() {
        String queryName = searchViewName.getQuery().toString().trim();
        String queryUser = searchViewUser.getQuery().toString().trim(); // Username do criador
        DeviceType selectedType = (DeviceType) spinnerDeviceType.getSelectedItem();
        Integer deviceTypeId = (selectedType != null && selectedType.DeviceTypeID != -1) ? selectedType.DeviceTypeID : null;

        boolean filterMissingMQTT = switchFilterMissingMQTT.isChecked();

        new Thread(() -> {
            List<Device> filteredDevices = deviceDao.searchDevices(queryName, deviceTypeId, queryUser, filterMissingMQTT);

            runOnUiThread(() -> {
                deviceAdapter.clear();
                deviceAdapter.addAll(filteredDevices);
                deviceAdapter.notifyDataSetChanged();
                textViewDeviceCount.setText("Number of Devices: " + filteredDevices.size());
            });
        }).start();
    }

    private void showDeleteConfirmationDialog() {
        new Thread(() -> {
            int permissionCount = deviceDao.countPermissionsForDevice(selectedDevice.DevicesID);

            runOnUiThread(() -> {
                if (permissionCount > 0) {
                    new AlertDialog.Builder(DeviceListActivity.this)
                            .setTitle("Cannot Delete Device")
                            .setMessage("This device is currently associated with permissions and cannot be deleted.")
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    // Criação e exibição do diálogo de confirmação na thread principal
                    new AlertDialog.Builder(DeviceListActivity.this)
                            .setTitle("Delete Device")
                            .setMessage("Are you sure you want to delete this device?")
                            .setPositiveButton("Delete", (dialog, which) -> deleteDevice())
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            });
        }).start();
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
}
