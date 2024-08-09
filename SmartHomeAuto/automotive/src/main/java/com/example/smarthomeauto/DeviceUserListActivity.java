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
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeviceUserListActivity extends AppCompatActivity {

    private ListView listViewDevices;
    private DeviceUserAdapter deviceUserAdapter;
    private List<UserDevice> deviceList;
    private UserDeviceDao deviceUserDao;
    private DeviceDao deviceDao;
    private DeviceTypeDao deviceTypeDao;
    private Spinner spinnerDeviceType;
    private Spinner spinnerPermissions;
    private TextView textViewDeviceCount;

    private Integer selectedDeviceTypeId = null;
    private String selectedPermission = "";
    private int guestId;
    private int userId;
    private String role;

    private Button buttonBack;
    private ImageButton buttonEdit;
    private ImageButton buttonDelete;
    private Button buttonAdd;
    private int selectedPosition = -1;
    private MqttManager mqttManager;
    private View rootView;


    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userdevicelist);

        Intent intent = getIntent();
        guestId = intent.getIntExtra("GUEST_ID", -1);
        userId = intent.getIntExtra("USER_ID", -1);
        role = intent.getStringExtra("USER_ROLE");

        rootView = findViewById(android.R.id.content);
        mqttManager = new MqttManager(this, userId);

        initializeViews();
        setupDatabase();
        setupListeners();
        loadDeviceTypes();
        loadPermissions();
        loadDevices(guestId);
    }

    private void initializeViews() {
        listViewDevices = findViewById(R.id.listViewDevices);
        textViewDeviceCount = findViewById(R.id.textViewDeviceCount);
        spinnerDeviceType = findViewById(R.id.spinnerDeviceType);
        spinnerPermissions = findViewById(R.id.spinnerPermissions);
        buttonAdd = findViewById(R.id.buttonAddDevice);
        buttonEdit = findViewById(R.id.buttonEdit);
        buttonBack = findViewById(R.id.buttonBack);
        buttonDelete = findViewById(R.id.buttonDelete);


    }


    private void setupDatabase() {
        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        deviceUserDao = db.userDeviceDao();
        deviceDao = db.deviceDao();
        deviceTypeDao = db.deviceTypeDao();
    }

    private void setupListeners() {
        buttonAdd.setOnClickListener(v -> {
            Intent addIntent = new Intent(DeviceUserListActivity.this, AddUserDeviceListActivity.class);
            addIntent.putExtra("GUEST_ID", guestId);
            addIntent.putExtra("USER_ID", userId);
            addIntent.putExtra("USER_ROLE", role);
            startActivity(addIntent);
        });

        buttonEdit.setOnClickListener(v -> {
            UserDevice selectedDevice = deviceUserAdapter.getItem(selectedPosition);
            if (selectedDevice != null) {

                new Thread(() -> {

                    AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
                    DeviceDao deviceDao = db.deviceDao();

                    String deviceName = deviceDao.getDeviceNameById(selectedDevice.getPermissionDeviceId());
                    Device device = deviceDao.getDeviceById(selectedDevice.getPermissionDeviceId());

                    DeviceTypeDao deviceTypeDao = db.deviceTypeDao();

                    if (device != null) {


                        int deviceTypeId = device.getTypeId();
                        String deviceTypeName = deviceTypeDao.getDeviceTypeNameById(deviceTypeId);

                        Intent editIntent = new Intent(DeviceUserListActivity.this, EditUserDeviceListActivity.class);
                        editIntent.putExtra("DEVICE_ID", selectedDevice.getPermissionDeviceId());
                        editIntent.putExtra("DEVICE_NAME", deviceName);
                        editIntent.putExtra("DEVICE_TYPE", deviceTypeName);
                        editIntent.putExtra("DEVICE_PERMISSION", selectedDevice.getPermissions());

                        editIntent.putExtra("GUEST_ID", guestId);
                        editIntent.putExtra("USER_ID", userId);
                        editIntent.putExtra("USER_ROLE", role);

                        runOnUiThread(() -> startActivity(editIntent));
                    } else {
                        runOnUiThread(() -> {
                            Snackbar.make(findViewById(android.R.id.content), "Device not found", Snackbar.LENGTH_SHORT)
                                    .setAction("Action", null)
                                    .show();
                        });
                    }
                }).start();
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Please select a permission to edit", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null)
                        .show();
            }
        });



        buttonBack.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("GUEST_ID", guestId);
            resultIntent.putExtra("USER_ID", userId);
            resultIntent.putExtra("USER_ROLE", role);
            setResult(RESULT_OK, resultIntent);
            finish();
        });


        buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog());


        spinnerDeviceType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DeviceType selectedDeviceType = (DeviceType) parent.getItemAtPosition(position);

                selectedDeviceTypeId = "All".equals(selectedDeviceType.DeviceTypeName) ? null : selectedDeviceType.getDeviceTypeID();
                filterDevices(((SearchView) findViewById(R.id.searchViewName)).getQuery().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedDeviceTypeId = null;
                filterDevices(((SearchView) findViewById(R.id.searchViewName)).getQuery().toString());
            }
        });


        spinnerPermissions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPermission = (String) parent.getItemAtPosition(position);
                selectedPermission = "All".equals(selectedPermission) ? "" : selectedPermission;
                filterDevices(((SearchView) findViewById(R.id.searchViewName)).getQuery().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedPermission = "";
                filterDevices(((SearchView) findViewById(R.id.searchViewName)).getQuery().toString());
            }
        });

        SearchView searchView = findViewById(R.id.searchViewName);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterDevices(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterDevices(newText);
                return true;
            }
        });

        listViewDevices.setOnItemClickListener((parent, view, position, id) -> {
            selectedPosition = position;
            deviceUserAdapter.setSelectedPosition(position);

            UserDevice selectedDevice = (UserDevice) parent.getItemAtPosition(position);
            int deviceId = selectedDevice.getPermissionDeviceId();

            Log.d("DeviceUserListActivity", "Selected position: " + position);
            Log.d("DeviceUserListActivity", "Selected device: " + selectedDevice);

            executorService.execute(() -> {
                String deviceName = deviceDao.getDeviceNameById(deviceId);

                runOnUiThread(() -> {
                    Snackbar.make(findViewById(android.R.id.content), "Selected Device: " + deviceName, Snackbar.LENGTH_SHORT).show();
                });
            });
        });


    }

    private void deleteUserDevice(UserDevice userDevice) {
        if (userDevice != null) {
            new Thread(() -> {
                try {
                    deviceUserDao.delete(userDevice);
                    runOnUiThread(() -> {
                        Snackbar.make(findViewById(android.R.id.content), "Permission deleted", Snackbar.LENGTH_SHORT).show();
                        loadDevices(guestId);
                    });
                } catch (Exception e) {
                    Log.e("DeviceUserListActivity", "Error deleting permission", e);
                    runOnUiThread(() -> Snackbar.make(findViewById(android.R.id.content), "Error deleting permission", Snackbar.LENGTH_SHORT).show());
                }
            }).start();
        } else {
            Snackbar.make(findViewById(android.R.id.content), "No permission selected", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void loadDeviceTypes() {
        executorService.execute(() -> {
            List<DeviceType> deviceTypes = deviceTypeDao.getAllDeviceTypes();
            List<DeviceType> deviceTypesWithAll = new ArrayList<>();
            deviceTypesWithAll.add(new DeviceType("All", "All"));
            deviceTypesWithAll.addAll(deviceTypes);

            runOnUiThread(() -> {
                ArrayAdapter<DeviceType> deviceTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, deviceTypesWithAll);
                deviceTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDeviceType.setAdapter(deviceTypeAdapter);
            });
        });
    }

    private void loadPermissions() {
        String[] permissions = {"All", "Read", "Control"};
        ArrayAdapter<String> permissionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, permissions);
        permissionsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPermissions.setAdapter(permissionsAdapter);
    }

    private void loadDevices(int guestId) {
        executorService.execute(() -> {
            List<UserDevice> userDevices = deviceUserDao.getDevicesByUserId(guestId);

            Log.d("DeviceUserListActivity", "Total de UserDevices recebidos: " + userDevices.size());
            for (UserDevice userDevice : userDevices) {
                Log.d("DeviceUserListActivity", "UserDevice recebido: " + userDevice.toString());
            }

            runOnUiThread(() -> {
                deviceUserAdapter = new DeviceUserAdapter(DeviceUserListActivity.this, userDevices, deviceTypeDao, deviceDao);
                listViewDevices.setAdapter(deviceUserAdapter);
                textViewDeviceCount.setText("Number of Permissions: " + userDevices.size());
            });
        });
    }

    private void filterDevices(String query) {
        executorService.execute(() -> {
            Log.d("DeviceUserListActivity", "Filtrando dispositivos com os seguintes parâmetros: " +
                    "guestId=" + guestId +
                    ", query=" + query +
                    ", deviceTypeId=" + selectedDeviceTypeId +
                    ", permission=" + selectedPermission);

            List<UserDevice> filteredUserDevices = deviceUserDao.getFilteredUserDevices(
                    guestId,
                    query.isEmpty() ? null : query,
                    selectedDeviceTypeId,
                    selectedPermission.isEmpty() ? null : selectedPermission
            );

            runOnUiThread(() -> {
                deviceUserAdapter.updateDeviceList(filteredUserDevices);
                textViewDeviceCount.setText("Number of Devices: " + filteredUserDevices.size());
            });
        });
    }
    private void showDeleteConfirmationDialog() {
        if (selectedPosition >= 0) {
            UserDevice selectedDevice = deviceUserAdapter.getItem(selectedPosition);
            if (selectedDevice != null) {
                new AlertDialog.Builder(this)
                        .setTitle("Confirm Deletion")
                        .setMessage("Are you sure you want to delete this permission?")
                        .setPositiveButton("Yes", (dialog, which) -> deleteUserDevice(selectedDevice))
                        .setNegativeButton("No", null)
                        .show();
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Selected device is not available", Snackbar.LENGTH_SHORT).show();
            }
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Please select a device to delete", Snackbar.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttManager != null) {
            mqttManager.disconnect();
        }
    }

}
