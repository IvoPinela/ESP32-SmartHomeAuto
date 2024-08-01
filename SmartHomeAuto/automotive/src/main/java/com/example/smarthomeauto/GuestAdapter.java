package com.example.smarthomeauto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.room.Room;

import java.util.List;

public class GuestAdapter extends ArrayAdapter<User> {

    private int selectedPosition = -1;
    private UserDao userDao; // Usando UserDao para acessar dados do usuário

    public GuestAdapter(Context context, List<User> guests) {
        super(context, 0, guests);

        // Initialize database and DAOs
        AppDatabase database = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        userDao = database.userDao();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.itemguest, parent, false);
        }

        User guest = getItem(position);

        TextView textViewUsername = convertView.findViewById(R.id.textViewUsername);
        Button buttonSeePermissions = convertView.findViewById(R.id.buttonSeePermissions);

        if (guest != null) {
            textViewUsername.setText("Username: " + guest.username);

            // Set background color based on some condition
            boolean isHighlighted = false;
            // Highlight if mqttUser, mqttPassword, managerUserId, or brokerID is null
            isHighlighted = guest.mqttUser == null || guest.mqttPassword == null || guest.managerUserId == null || guest.brokerID == null;

            convertView.setBackgroundColor(isHighlighted ? Color.YELLOW : Color.WHITE);

            buttonSeePermissions.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), PermissionsActivity.class);
                intent.putExtra("USER_ID", guest.id);
                getContext().startActivity(intent);
            });

        } else {
            textViewUsername.setText("N/A");
        }

        // Highlight selected item
        if (position == selectedPosition) {
            convertView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorSelectedItem));
        } else {
            // Keep previously set background color
            convertView.setBackgroundColor(guest != null && (guest.mqttUser == null || guest.mqttPassword == null || guest.managerUserId == null || guest.brokerID == null) ? Color.YELLOW : Color.WHITE);
        }

        return convertView;
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    public void updateGuestList(List<User> newGuests) {
        clear();
        addAll(newGuests);
        notifyDataSetChanged();
    }
}
