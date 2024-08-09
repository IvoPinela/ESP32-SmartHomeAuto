package com.example.smarthomeauto;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;


import androidx.core.content.ContextCompat;

import java.util.List;

public class GuestAdapter extends ArrayAdapter<User> {

    private Context context;
    private List<User> guestList;
    private int selectedPosition = -1;

    public GuestAdapter(Context context, List<User> guestList) {
        super(context, 0, guestList);
        this.context = context;
        this.guestList = guestList != null ? guestList : new ArrayList<>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.itemguest, parent, false);
        }

        User guest = guestList.get(position);

        TextView textViewGuestName = convertView.findViewById(R.id.textViewUsername);
        textViewGuestName.setText(guest.Username);


        if (position == selectedPosition) {
            convertView.setBackgroundColor(ContextCompat.getColor(context, R.color.colorSelectedItem));
        } else {
            convertView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        }

        return convertView;
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    public User getSelectedGuest() {
        if (selectedPosition != -1 && selectedPosition < guestList.size()) {
            return guestList.get(selectedPosition);
        }
        return null;
    }
}
