package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class GuestListActivity extends AppCompatActivity {

    private static final int REQUEST_ADD_GUEST = 1;
    private static final int REQUEST_EDIT_GUEST = 2;

    private ListView listViewGuests;
    private GuestAdapter guestAdapter;
    private List<User> guestList;
    private UserDao userDao;
    private User selectedGuest;
    private SearchView searchViewUsername;
    private TextView textViewGuestCount;
    private int managerUserId;
    private MqttManager mqttManager;
    private View rootView;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guestlist);

        rootView = findViewById(android.R.id.content);

        Button buttonBack = findViewById(R.id.buttonBack);
        Button buttonAddGuest = findViewById(R.id.buttonAddUser);
        ImageButton buttonDelete = findViewById(R.id.buttonDelete);
        ImageButton buttonEditGuest = findViewById(R.id.buttonEdit);
        ImageButton buttonPermissions= findViewById(R.id.buttonPermissions);
        searchViewUsername = findViewById(R.id.searchViewUsername);
        listViewGuests = findViewById(R.id.listViewGuest);
        textViewGuestCount = findViewById(R.id.textViewUserCount);


        // Obtendo o ID do gerente do Intent
        Intent intent = getIntent();
        managerUserId = intent.getIntExtra("USER_ID", -1);
        role= intent.getStringExtra("USER_ROLE");
        mqttManager = new MqttManager(this, managerUserId);
        userDao = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build().userDao();

        buttonBack.setOnClickListener(v -> finish());

        buttonAddGuest.setOnClickListener(v -> {
            Intent addGuestIntent = new Intent(GuestListActivity.this, AddGuestActivity.class);
            addGuestIntent.putExtra("USER_ID", managerUserId);
            addGuestIntent.putExtra("USER_ROLE", role);
            startActivityForResult(addGuestIntent, REQUEST_ADD_GUEST);
        });

        buttonPermissions.setOnClickListener(v -> {
            if (selectedGuest != null) {
                Intent intent2 = new Intent(GuestListActivity.this, DeviceUserListActivity.class);
                intent2.putExtra("GUEST_ID", selectedGuest.id);
                intent2.putExtra("USER_ID", managerUserId);
                intent2.putExtra("USER_ROLE", role);
                startActivity(intent2);
            } else {
                Snackbar.make(findViewById(android.R.id.content), "No guest selected", Snackbar.LENGTH_SHORT).show();
            }
        });


        buttonEditGuest.setOnClickListener(v -> {
            if (selectedGuest != null) {
                Intent editGuestIntent = new Intent(GuestListActivity.this, EditGuestActivity.class);
                editGuestIntent.putExtra("user", selectedGuest);
                editGuestIntent.putExtra("USER_ID", managerUserId);
                editGuestIntent.putExtra("USER_ROLE", role);
                startActivityForResult(editGuestIntent, REQUEST_EDIT_GUEST);
            } else {
                Snackbar.make(findViewById(android.R.id.content), "No guest selected", Snackbar.LENGTH_SHORT).show();
            }
        });

        buttonDelete.setOnClickListener(v -> {
            if (selectedGuest != null) {
                showDeleteConfirmationDialog();
            } else {
                Snackbar.make(findViewById(android.R.id.content), "No guest selected", Snackbar.LENGTH_SHORT).show();
            }
        });

        listViewGuests.setOnItemClickListener((parent, view, position, id) -> {
            selectedGuest =  guestList.get(position);
            guestAdapter.setSelectedPosition(position);
            Snackbar.make(findViewById(android.R.id.content), "Selected: " + selectedGuest.username, Snackbar.LENGTH_SHORT).show();
        });

        setupSearchView();
        loadGuests();
    }

    private void setupSearchView() {
        searchViewUsername.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterGuests(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterGuests(newText);
                return true;
            }
        });
    }

    private void loadGuests() {
        new Thread(() -> {
            guestList = userDao.getAllGuests(managerUserId);
            runOnUiThread(() -> {
                guestAdapter = new GuestAdapter(GuestListActivity.this, guestList);
                listViewGuests.setAdapter(guestAdapter);
                textViewGuestCount.setText("Number of Guests: " + guestList.size());
            });
        }).start();
    }

    private void filterGuests(String query) {
        new Thread(() -> {
            List<User> allGuests = userDao.getAllGuests(managerUserId);

            if (query != null && !query.isEmpty()) {
                String searchQuery = "%" + query + "%";
                allGuests.removeIf(user -> !user.username.toLowerCase().contains(query.toLowerCase()));
            }
            runOnUiThread(() -> {
                guestAdapter.clear();
                guestAdapter.addAll(allGuests);
                guestAdapter.notifyDataSetChanged();
                textViewGuestCount.setText("Number of Guests: " + allGuests.size());
            });
        }).start();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Guest")
                .setMessage("Are you sure you want to delete this guest?")
                .setPositiveButton("Delete", (dialog, which) -> deleteGuest())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteGuest() {
        if (selectedGuest != null) {
            new Thread(() -> {
                userDao.delete(selectedGuest);
                runOnUiThread(() -> {
                    Snackbar.make(findViewById(android.R.id.content), "Guest deleted", Snackbar.LENGTH_SHORT).show();
                    loadGuests();
                    selectedGuest = null;
                });
            }).start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_GUEST || requestCode == REQUEST_EDIT_GUEST) {
            if (resultCode == RESULT_OK) {
                loadGuests();
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
}
