package com.example.smarthomeauto;

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

import java.util.ArrayList;
import java.util.List;

public class UserListActivity extends AppCompatActivity {

    private String userRole;
    private static final int REQUEST_ADD_USER = 1;
    private static final int REQUEST_EDIT_USER = 2;

    private ListView listViewUsers;
    private UserAdapter userAdapter;
    private List<User> userList;
    private UserDao userDao;
    private BrokerDao brokerDao;
    private List<String> userTypeList;
    private List<String> brokerList;
    private User selectedUser;
    private Spinner spinnerUserType;
    private Spinner spinnerBroker;
    private SearchView searchViewUsername;
    private Switch switchFilterNullFields;
    private TextView textViewUserCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userlist);

        Button buttonBack = findViewById(R.id.buttonBack);
        Button buttonAddUser = findViewById(R.id.buttonAddUser);
        ImageButton buttonDelete = findViewById(R.id.buttonDelete);
        ImageButton buttonEditUser = findViewById(R.id.buttonEdit);
        switchFilterNullFields = findViewById(R.id.switchFilterNullFields);
        spinnerUserType = findViewById(R.id.spinnerUserRole);
        spinnerBroker = findViewById(R.id.spinnerBroker);
        searchViewUsername = findViewById(R.id.searchViewUsername);
        listViewUsers = findViewById(R.id.listViewUsers);
        textViewUserCount = findViewById(R.id.textViewUserCount);

        // Get user role from Intent
        Intent intent2 = getIntent();
        if (intent2 != null) {
            userRole = intent2.getStringExtra("USER_ROLE");
            Log.d("UserListActivity", "User Role: " + userRole);
        }

        AppDatabase database = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        userDao = database.userDao();
        brokerDao = database.brokerDao();

        buttonBack.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("USER_ROLE", userRole);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        buttonAddUser.setOnClickListener(v -> {
            Intent intent = new Intent(UserListActivity.this, AddUserActivity.class);
            intent.putExtra("USER_ROLE", userRole);
            startActivityForResult(intent, REQUEST_ADD_USER);
        });

        buttonEditUser.setOnClickListener(v -> {
            if (selectedUser != null) {
                Intent intent = new Intent(UserListActivity.this, EditUserActivity.class);
                intent.putExtra("user", selectedUser);
                intent.putExtra("USER_ROLE", userRole);
                startActivityForResult(intent, REQUEST_EDIT_USER);
            } else {
                Snackbar.make(findViewById(android.R.id.content), "No user selected", Snackbar.LENGTH_SHORT).show();
            }
        });

        buttonDelete.setOnClickListener(v -> {
            if (selectedUser != null) {
                showDeleteConfirmationDialog();
            } else {
                Snackbar.make(findViewById(android.R.id.content), "No user selected", Snackbar.LENGTH_SHORT).show();
            }
        });

        switchFilterNullFields.setOnCheckedChangeListener((buttonView, isChecked) -> filterUsers());

        listViewUsers.setOnItemClickListener((parent, view, position, id) -> {
            selectedUser = userList.get(position);
            userAdapter.setSelectedPosition(position);
            Snackbar.make(findViewById(android.R.id.content), "Selected: " + selectedUser.Username, Snackbar.LENGTH_SHORT).show();
        });

        setupSpinners();
        setupSearchViews();
        loadUsers();
    }

    private void setupSpinners() {
        userTypeList = new ArrayList<>();
        userTypeList.add("All");
        userTypeList.add("user");
        userTypeList.add("guest");

        if ("adminmaster".equals(userRole)) {
            userTypeList.add("admin");
        }

        ArrayAdapter<String> userTypeAdapter = new ArrayAdapter<>(UserListActivity.this, android.R.layout.simple_spinner_item, userTypeList);
        userTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserType.setAdapter(userTypeAdapter);
        spinnerUserType.setSelection(0);

        spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterUsers();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                filterUsers();
            }
        });

        new Thread(() -> {
            List<Broker> brokers = brokerDao.getAllBrokers();
            brokerList = new ArrayList<>();
            brokerList.add("All");

            for (Broker broker : brokers) {
                brokerList.add(broker.ClusterUrl + ":" + broker.Port);
            }

            runOnUiThread(() -> {
                ArrayAdapter<String> brokerAdapter = new ArrayAdapter<>(UserListActivity.this, android.R.layout.simple_spinner_item, brokerList);
                brokerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerBroker.setAdapter(brokerAdapter);
                spinnerBroker.setSelection(0);

                spinnerBroker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        filterUsers();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        filterUsers();
                    }
                });
            });
        }).start();
    }

    private void setupSearchViews() {
        searchViewUsername.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterUsers();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterUsers();
                return true;
            }
        });
    }

    private void loadUsers() {
        new Thread(() -> {
            List<User> users;
            if ("adminmaster".equals(userRole)) {
                users = userDao.getAllUsers();
            } else {
                users = userDao.getAllUsersExcludingAdmin();
            }
            runOnUiThread(() -> {
                userList = users;
                userAdapter = new UserAdapter(UserListActivity.this, userList);
                listViewUsers.setAdapter(userAdapter);
            });
        }).start();
    }

    private void filterUsers() {

        String queryUsername = searchViewUsername.getQuery().toString().trim();
        String selectedType = (String) spinnerUserType.getSelectedItem();
        String userType = "All".equals(selectedType) ? null : selectedType;
        String selectedBroker = (String) spinnerBroker.getSelectedItem();
        String broker = "All".equals(selectedBroker) ? null : selectedBroker;
        boolean showNullFieldsOnly = switchFilterNullFields.isChecked();

        new Thread(() -> {
            List<User> filteredUsers;

            if ("adminmaster".equals(userRole)) {
                filteredUsers = userDao.searchUsers(queryUsername, userType, broker);
            } else if ("admin".equals(userRole)) {
                filteredUsers = userDao.searchUsersExcludingAdmin(queryUsername, userType, broker);
            } else {
                filteredUsers = userDao.searchUsers(queryUsername, userType, broker);
            }


            if (showNullFieldsOnly) {
                filteredUsers = filterUsersWithNullFields(filteredUsers, userType);
                filteredUsers = filterByUserType(filteredUsers);
            }

            List<User> finalFilteredUsers = filteredUsers;
            runOnUiThread(() -> {
                userAdapter.clear();
                userAdapter.addAll(finalFilteredUsers);
                userAdapter.notifyDataSetChanged();
                textViewUserCount.setText("Number of Users: " + finalFilteredUsers.size());
            });
        }).start();
    }

    private List<User> filterUsersWithNullFields(List<User> users, String userType) {
        List<User> result = new ArrayList<>();
        for (User user : users) {
            boolean hasNullFields = false;

            if ("guest".equals(user.Role)) {
                if (user.MqttUser == null || user.MqttPassword == null || user.ManagerUserId == null || user.UserBrokerID == null) {
                    hasNullFields = true;
                }
            } else if ("user".equals(user.Role)) {
                if (user.MqttUser == null || user.MqttPassword == null || user.UserBrokerID == null) {
                    hasNullFields = true;
                }
            } else {
                continue;
            }

            if (hasNullFields) {
                result.add(user);
            }
        }
        return result;
    }

    private List<User> filterByUserType(List<User> users) {
        List<User> result = new ArrayList<>();
        for (User user : users) {
            if ("user".equals(user.Role) || "guest".equals(user.Role)) {
                result.add(user);
            }
        }
        return result;
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(UserListActivity.this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this user?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    new Thread(() -> {
                        userDao.delete(selectedUser);
                        runOnUiThread(() -> {
                            userList.remove(selectedUser);
                            userAdapter.notifyDataSetChanged();
                            selectedUser = null;
                            Snackbar.make(findViewById(android.R.id.content), "User deleted", Snackbar.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadUsers();
        }
    }
}
