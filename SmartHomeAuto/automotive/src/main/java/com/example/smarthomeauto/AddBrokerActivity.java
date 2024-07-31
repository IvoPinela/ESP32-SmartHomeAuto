package com.example.smarthomeauto;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

public class AddBrokerActivity extends AppCompatActivity {

    private EditText editTextClusterURL, editTextPort;
    private Button buttonSaveBroker, buttonBack;
    private BrokerDao brokerDao;
    private String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_broker);

        Intent intent2 = getIntent();
        if (intent2 != null) {
            userRole = intent2.getStringExtra("USER_ROLE");
            Log.d("UserListActivity", "User Role: " + userRole);
        }
        editTextClusterURL = findViewById(R.id.editTextClusterURL);
        editTextPort = findViewById(R.id.editTextPort);
        buttonSaveBroker = findViewById(R.id.buttonSaveBroker);
        buttonBack = findViewById(R.id.buttonBack);

        AppDatabase db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        brokerDao = db.brokerDao();

        buttonBack.setOnClickListener(v -> {
            Intent intent = new Intent(AddBrokerActivity.this, BrokerListActivity.class);
            intent.putExtra("USER_ROLE", userRole);
            startActivity(intent);
            finish();
        });

        buttonSaveBroker.setOnClickListener(v -> saveBroker());
    }

    private void saveBroker() {
        boolean hasError = false;
        String clusterURL = editTextClusterURL.getText().toString().trim();
        String portString = editTextPort.getText().toString().trim();

        if (clusterURL.isEmpty()) {
            editTextClusterURL.setError("Cluster URL is required");
            hasError = true;
        }
        if (portString.isEmpty()) {
            editTextPort.setError("Port is required");
            hasError = true;
        }

        if (hasError) {
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portString);
        } catch (NumberFormatException e) {
            editTextPort.setError("Invalid port number");
            return;
        }

        new Thread(() -> {
            // Check if a broker with the same ClusterURL and PORT already exists
            Broker existingBroker = brokerDao.getBrokerByUrlAndPort(clusterURL, port);
            if (existingBroker != null) {
                runOnUiThread(() -> {
                    showAlert("Broker with the same Cluster URL and Port already exists!");
                });
            } else {
                Broker newBroker = new Broker(clusterURL, port);
                brokerDao.insert(newBroker);
                runOnUiThread(() -> {
                    showAlert("Broker saved!");
                    Intent intent = new Intent(AddBrokerActivity.this, BrokerListActivity.class);
                    intent.putExtra("USER_ROLE", userRole);
                    setResult(RESULT_OK, intent);
                    finish();
                });
            }
        }).start();
    }

    private void showAlert(String message) {
        new AlertDialog.Builder(AddBrokerActivity.this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}
