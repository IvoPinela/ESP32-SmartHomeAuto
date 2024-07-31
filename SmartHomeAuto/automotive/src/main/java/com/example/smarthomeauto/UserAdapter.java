package com.example.smarthomeauto;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.room.Room;

import java.util.List;

public class UserAdapter extends ArrayAdapter<User> {

    private int selectedPosition = -1;
    private UserDao userDao;
    private BrokerDao brokerDao;

    public UserAdapter(Context context, List<User> users) {
        super(context, 0, users);

        // Initialize database and DAOs
        AppDatabase database = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        userDao = database.userDao();
        brokerDao = database.brokerDao();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.itemuser, parent, false);
        }

        User user = getItem(position);

        TextView textViewUsername = convertView.findViewById(R.id.textViewUsername);
        TextView textViewRole = convertView.findViewById(R.id.textViewRole);
        TextView textViewMqttUser = convertView.findViewById(R.id.textViewMqttUser);
        TextView textViewMqttPassword = convertView.findViewById(R.id.textViewMqttPassword);
        TextView textViewManagerUserName = convertView.findViewById(R.id.textViewManagerUserId);
        TextView textViewBrokerURL = convertView.findViewById(R.id.textViewBrokerId);

        if (user != null) {
            textViewUsername.setText("Username: " + user.username);
            textViewRole.setText("Role: " + user.role);
            textViewMqttUser.setText("MQTT User: " + (user.mqttUser != null ? user.mqttUser : "N/A"));
            textViewMqttPassword.setText("MQTT Password: " + (user.mqttPassword != null ? user.mqttPassword : "N/A"));

            // Fetch manager user info
            new Thread(() -> {
                User managerUser = user.managerUserId != null ? userDao.getUserById(user.managerUserId) : null;
                String managerUserName = (managerUser != null) ? managerUser.username : "N/A";
                ((Activity) getContext()).runOnUiThread(() -> {
                    textViewManagerUserName.setText("Manager: " + managerUserName);
                });
            }).start();

            // Fetch broker info
            new Thread(() -> {
                Broker broker = user.brokerID != null ? brokerDao.getBrokerById(user.brokerID) : null;
                String brokerURL = (broker != null) ? broker.ClusterURL + ":" + broker.PORT : "N/A";
                ((Activity) getContext()).runOnUiThread(() -> {
                    textViewBrokerURL.setText("Broker: " + brokerURL);
                });
            }).start();

            // Determine if the item needs to be highlighted
            boolean isHighlighted = false;
            if ("user".equals(user.role)) {
                // Highlight if mqttUser, mqttPassword, or brokerID is null
                isHighlighted = user.mqttUser == null || user.mqttPassword == null || user.brokerID == null;
            } else if ("guest".equals(user.role)) {
                // Highlight if mqttUser, mqttPassword, managerUserId, or brokerID is null
                isHighlighted = user.mqttUser == null || user.mqttPassword == null || user.managerUserId == null || user.brokerID == null;
            }

            // Set background color
            convertView.setBackgroundColor(isHighlighted ? Color.YELLOW : Color.WHITE);

        } else {
            textViewUsername.setText("N/A");
            textViewRole.setText("N/A");
            textViewMqttUser.setText("N/A");
            textViewMqttPassword.setText("N/A");
            textViewManagerUserName.setText("N/A");
            textViewBrokerURL.setText("N/A");

            // Set background color to yellow for items with no user data
            convertView.setBackgroundColor(Color.YELLOW);
        }

        // Highlight selected item
        if (position == selectedPosition) {
            convertView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorSelectedItem));
        } else {
            // Keep the previously set background color if it was highlighted
            if (user != null && ("user".equals(user.role) || "guest".equals(user.role)) &&
                    (user.mqttUser == null || user.mqttPassword == null || (user.managerUserId == null && "guest".equals(user.role)) || user.brokerID == null)) {
                convertView.setBackgroundColor(Color.YELLOW);
            } else {
                convertView.setBackgroundColor(Color.WHITE);
            }
        }

        return convertView;
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    public void updateUserList(List<User> newUsers) {
        clear();
        addAll(newUsers);
        notifyDataSetChanged();
    }
}
