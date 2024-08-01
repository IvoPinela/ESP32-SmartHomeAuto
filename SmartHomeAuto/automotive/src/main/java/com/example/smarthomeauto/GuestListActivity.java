package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
    private UserAdapter userAdapter;
    private List<User> guestList;
    private UserDao userDao;
    private User selectedUser;
    private SearchView searchViewUsername;
    private TextView textViewGuestCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.guestlist);

        Button buttonBack = findViewById(R.id.buttonBack);
        Button buttonAddGuest = findViewById(R.id.buttonAddUser);
        ImageButton buttonDelete = findViewById(R.id.buttonDelete);
        ImageButton buttonEditGuest = findViewById(R.id.buttonEdit);
        searchViewUsername = findViewById(R.id.searchViewUsername);
        listViewGuests = findViewById(R.id.listViewUsers);
        textViewGuestCount = findViewById(R.id.textViewUserCount);
        // Get manager user ID from Intent
        Intent intent = getIntent();
        int managerUserId = intent.getIntExtra("USER_ID", -1);
        Log.d("GuestListActivity", "Manager User ID: " + managerUserId);

        AppDatabase database = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        userDao = database.userDao();

        buttonBack.setOnClickListener(v -> finish());

        buttonAddGuest.setOnClickListener(v -> {
            Intent addGuestIntent = new Intent(GuestListActivity.this, AddUserActivity.class);
            addGuestIntent.putExtra("USER_ROLE", "guest");
            startActivityForResult(addGuestIntent, REQUEST_ADD_GUEST);
        });

        buttonEditGuest.setOnClickListener(v -> {
            if (selectedUser != null) {
                Intent editGuestIntent = new Intent(GuestListActivity.this, EditUserActivity.class);
                editGuestIntent.putExtra("user", selectedUser);
                startActivityForResult(editGuestIntent, REQUEST_EDIT_GUEST);
            } else {
                Snackbar.make(findViewById(android.R.id.content), "No guest selected", Snackbar.LENGTH_SHORT).show();
            }
        });

        buttonDelete.setOnClickListener(v -> {
            if (selectedUser != null) {
                showDeleteConfirmationDialog();
            } else {
                Snackbar.make(findViewById(android.R.id.content), "No guest selected", Snackbar.LENGTH_SHORT).show();
            }
        });

        searchViewUsername.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterGuests(query, managerUserId);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterGuests(newText, managerUserId);
                return true;
            }
        });

        loadGuests(managerUserId);
    }

    private void loadGuests(int managerUserId) {
        new Thread(() -> {
            List<User> guests = userDao.getAllGuests(managerUserId);
            runOnUiThread(() -> {
                guestList = guests;
                userAdapter = new UserAdapter(GuestListActivity.this, guestList);
                listViewGuests.setAdapter(userAdapter);
                textViewGuestCount.setText("Number of Guests: " + guestList.size());
            });
        }).start();
    }

    private void filterGuests(String query, int managerUserId) {
        new Thread(() -> {
            List<User> filteredGuests = (List<User>) userDao.getUserByUsername(query);
            filteredGuests.removeIf(user -> user.managerUserId != managerUserId);

            runOnUiThread(() -> {
                userAdapter.clear();
                userAdapter.addAll(filteredGuests);
                userAdapter.notifyDataSetChanged();

                textViewGuestCount.setText("Number of Guests: " + filteredGuests.size());
            });
        }).start();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(GuestListActivity.this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this guest?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    new Thread(() -> {
                        userDao.delete(selectedUser);
                        runOnUiThread(() -> {
                            guestList.remove(selectedUser);
                            userAdapter.notifyDataSetChanged();
                            selectedUser = null;
                            Snackbar.make(findViewById(android.R.id.content), "Guest deleted", Snackbar.LENGTH_SHORT).show();
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
            Intent intent = getIntent();
            int managerUserId = intent.getIntExtra("MANAGER_USER_ID", -1);
            loadGuests(managerUserId);
        }
    }
}
