package com.example.smarthomeauto;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.room.Room;

public class EditBrokerActivity extends Activity {

    private EditText editTextClusterURL;
    private EditText editTextPort;
    private Button buttonSaveBroker;
    private Button buttonBack;
    private Broker broker;
    private BrokerDao brokerDao;
    private String userRole;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_broker);

        Intent intent3 = getIntent();
        if (intent3!= null) {
            userRole = intent3.getStringExtra("USER_ROLE");
            Log.d("UserListActivity", "User Role: " + userRole);
        }

        TextView formTitle = findViewById(R.id.formTitle);
        editTextClusterURL = findViewById(R.id.editTextClusterURL);
        editTextPort = findViewById(R.id.editTextPort);
        buttonSaveBroker = findViewById(R.id.buttonSaveBroker);
        buttonBack = findViewById(R.id.buttonBack);

        AppDatabase database = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        brokerDao = database.brokerDao();

        Intent intent = getIntent();
        if (intent.hasExtra("broker")) {
            broker = (Broker) intent.getSerializableExtra("broker");
            formTitle.setText("Edit Broker");
            editTextClusterURL.setText(broker.ClusterUrl);
            editTextPort.setText(String.valueOf(broker.Port));
        } else {
            formTitle.setText("Add New Broker");
        }

        buttonSaveBroker.setOnClickListener(v -> saveBroker());

        buttonBack.setOnClickListener(v -> {
            Intent intent2 = new Intent(EditBrokerActivity.this, BrokerListActivity.class);
            startActivity(intent2);
            finish();
        });
    }

    private void saveBroker() {
        String clusterURL = editTextClusterURL.getText().toString().trim();
        String portString = editTextPort.getText().toString().trim();

        boolean hasError = false;

        if (clusterURL.isEmpty()) {
            editTextClusterURL.setError("Cluster URL is required");
            hasError = true;
        } else if (clusterURL.length() > 51) {
            editTextClusterURL.setError("Cluster URL must be 51 characters or less");
            hasError = true;
        }

        int port;
        port = Integer.parseInt(portString);
        if (portString.isEmpty()) {
            editTextPort.setError("Port is required");
            hasError = true;
        } else {

            try {
                if (port < 1000 || port > 9999) {
                    editTextPort.setError("Port must be between 1000 and 9999");
                    hasError = true;
                }
            } catch (NumberFormatException e) {
                editTextPort.setError("Invalid port number");
                hasError = true;
            }
        }
        if (hasError) {
            return;
        }

        new Thread(() -> {
            if (broker != null) {
                // Check for duplicate brokers excluding the one being edited
                Broker existingBroker = brokerDao.getBrokerByUrlAndPortExcludingId(clusterURL, port, broker.BrokerID);
                if (existingBroker != null) {
                    runOnUiThread(() -> {
                        showAlert("Broker with the same Cluster URL and Port already exists!");
                    });
                } else {
                    // Update the existing broker
                    broker.ClusterUrl = clusterURL;
                    broker.Port = port;

                    brokerDao.update(broker);
                    runOnUiThread(() -> {
                        showAlert("Broker updated!");
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("broker", broker);
                        resultIntent.putExtra("USER_ROLE", userRole);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    });
                }
            } else {
                // Check if a broker with the same ClusterURL and PORT already exists
                Broker existingBroker = brokerDao.getBrokerByUrlAndPort(clusterURL, port);
                if (existingBroker != null) {
                    runOnUiThread(() -> {
                        showAlert("Broker with the same Cluster URL and Port already exists!");
                    });
                } else {
                    // Insert new broker
                    Broker newBroker = new Broker(clusterURL, port);
                    brokerDao.insert(newBroker);
                    runOnUiThread(() -> {
                        showAlert("Broker added!");
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("broker", newBroker);
                        resultIntent.putExtra("USER_ROLE", userRole);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    });
                }
            }
        }).start();
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(EditBrokerActivity.this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}
