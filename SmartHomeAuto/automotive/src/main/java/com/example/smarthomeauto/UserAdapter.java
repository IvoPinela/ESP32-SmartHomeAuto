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
            textViewUsername.setText("Username: " + user.Username);
            textViewRole.setText("Role: " + user.Role);
            textViewMqttUser.setText("MQTT User: " + (user.MqttUser != null ? user.MqttUser : "N/A"));
            textViewMqttPassword.setText("MQTT Password: " + (user.MqttPassword != null ? user.MqttPassword : "N/A"));

            // Fetch manager user info
            new Thread(() -> {
                User managerUser = user.ManagerUserId != null ? userDao.getUserById(user.ManagerUserId) : null;
                String managerUserName = (managerUser != null) ? managerUser.Username : "N/A";
                ((Activity) getContext()).runOnUiThread(() -> {
                    textViewManagerUserName.setText("Manager: " + managerUserName);
                });
            }).start();

            // Fetch broker info
            new Thread(() -> {
                Broker broker = user.UserBrokerID != null ? brokerDao.getBrokerById(user.UserBrokerID) : null;
                String brokerURL = (broker != null) ? broker.ClusterUrl + ":" + broker.Port : "N/A";
                ((Activity) getContext()).runOnUiThread(() -> {
                    textViewBrokerURL.setText("Broker: " + brokerURL);
                });
            }).start();

            // Determine if the item needs to be highlighted
            boolean isHighlighted = false;
            if ("user".equals(user.Role)) {
                // Highlight if mqttUser, mqttPassword, or brokerID is null
                isHighlighted = user.MqttUser == null || user.MqttPassword == null || user.UserBrokerID == null;
            } else if ("guest".equals(user.Role)) {
                // Highlight if mqttUser, mqttPassword, managerUserId, or brokerID is null
                isHighlighted = user.MqttUser == null || user.MqttPassword == null || user.ManagerUserId == null || user.UserBrokerID == null;
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
            if (user != null && ("user".equals(user.Role) || "guest".equals(user.Role)) &&
                    (user.MqttUser == null || user.MqttPassword == null || (user.ManagerUserId == null && "guest".equals(user.Role)) || user.UserBrokerID == null)) {
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
