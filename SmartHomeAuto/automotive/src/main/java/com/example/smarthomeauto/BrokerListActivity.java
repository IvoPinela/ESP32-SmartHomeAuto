package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class BrokerListActivity extends AppCompatActivity {

    private static final int REQUEST_ADD_BROKER = 1;
    private static final int REQUEST_EDIT_BROKER = 2;

    private ListView listViewBrokers;
    private BrokerAdapter brokerAdapter;
    private List<Broker> brokerList;
    private BrokerDao brokerDao;
    private Broker selectedBroker;
    private SearchView searchViewClusterURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.brokerlist);

        Button buttonBack = findViewById(R.id.buttonBack);
        Button buttonAddBroker = findViewById(R.id.buttonAddBroker);
        ImageButton buttonDelete = findViewById(R.id.buttonDelete);
        ImageButton buttonEditBroker = findViewById(R.id.buttonEdit);
        searchViewClusterURL = findViewById(R.id.searchViewClusterURL);
        listViewBrokers = findViewById(R.id.listViewBrokers);

        brokerDao = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build().brokerDao();

        buttonBack.setOnClickListener(v -> finish());

        buttonAddBroker.setOnClickListener(v -> {
            Intent intent = new Intent(BrokerListActivity.this, AddBrokerActivity.class);
            startActivityForResult(intent, REQUEST_ADD_BROKER);
        });

        buttonEditBroker.setOnClickListener(v -> {
            if (selectedBroker != null) {
                Intent intent = new Intent(BrokerListActivity.this, EditBrokerActivity.class);
                intent.putExtra("broker",selectedBroker);
                startActivityForResult(intent, REQUEST_EDIT_BROKER);
            } else {
                Snackbar.make(findViewById(android.R.id.content), "No broker selected", Snackbar.LENGTH_SHORT).show();
            }
        });

        buttonDelete.setOnClickListener(v -> {
            if (selectedBroker != null) {
                showDeleteConfirmationDialog();
            } else {
                Snackbar.make(findViewById(android.R.id.content), "No broker selected", Snackbar.LENGTH_SHORT).show();
            }
        });

        listViewBrokers.setOnItemClickListener((parent, view, position, id) -> {
            selectedBroker = brokerList.get(position);
            brokerAdapter.setSelectedPosition(position);
            Snackbar.make(findViewById(android.R.id.content), "Selected: " + selectedBroker.ClusterURL, Snackbar.LENGTH_SHORT).show();
        });

        setupSearchView();
        loadBrokers();
    }

    private void setupSearchView() {
        searchViewClusterURL.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterBrokers();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterBrokers();
                return true;
            }
        });
    }

    private void loadBrokers() {
        new Thread(() -> {
            brokerList = brokerDao.getAllBrokers();
            runOnUiThread(() -> {
                brokerAdapter = new BrokerAdapter(BrokerListActivity.this, brokerList);
                listViewBrokers.setAdapter(brokerAdapter);
            });
        }).start();
    }

    private void filterBrokers() {
        String queryClusterURL = searchViewClusterURL.getQuery().toString().trim();

        new Thread(() -> {
            List<Broker> filteredBrokers = brokerDao.searchBrokers(queryClusterURL);
            runOnUiThread(() -> {
                brokerAdapter.clear();
                brokerAdapter.addAll(filteredBrokers);
                brokerAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Broker")
                .setMessage("Are you sure you want to delete this broker?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBroker())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBroker() {
        if (selectedBroker != null) {
            new Thread(() -> {
                brokerDao.delete(selectedBroker); // Método fictício
                runOnUiThread(() -> {
                    Snackbar.make(findViewById(android.R.id.content), "Broker deleted", Snackbar.LENGTH_SHORT).show();
                    loadBrokers();
                    selectedBroker = null;
                });
            }).start();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_BROKER || requestCode == REQUEST_EDIT_BROKER) {
            if (resultCode == RESULT_OK) {
                loadBrokers();
            }
        }
    }
}
