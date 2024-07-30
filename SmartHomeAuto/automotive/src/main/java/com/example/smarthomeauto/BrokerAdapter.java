package com.example.smarthomeauto;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

public class BrokerAdapter extends ArrayAdapter<Broker> {

    private int selectedPosition = -1;  // Track the selected position

    public BrokerAdapter(Context context, List<Broker> brokers) {
        super(context, 0, brokers);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.itembroker, parent, false);
        }

        Broker broker = getItem(position);

        TextView textViewClusterURL = convertView.findViewById(R.id.textViewClusterURL);
        TextView textViewPort = convertView.findViewById(R.id.textViewPort);

        if (broker != null) {
            textViewClusterURL.setText("Cluster URL: " + broker.ClusterURL);
            textViewPort.setText("Port: " + broker.PORT);
        }

        // Highlight selected item
        if (position == selectedPosition) {
            convertView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorSelectedItem));
        } else {
            convertView.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.white));
        }

        return convertView;
    }

    // Method to set the selected position
    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    // Method to update the list of brokers
    public void updateBrokerList(List<Broker> newBrokers) {
        clear();
        addAll(newBrokers);
        notifyDataSetChanged();
    }
}
