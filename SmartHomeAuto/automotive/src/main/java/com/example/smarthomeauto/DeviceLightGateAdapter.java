package com.example.smarthomeauto;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceLightGateAdapter extends RecyclerView.Adapter<DeviceLightGateAdapter.DeviceViewHolder> {

    private static final String TAG = "DeviceLightAdapter";
    private final List<Device> devices;
    private final OnDeviceSwitchChangedListener listener;
    private final Map<String, Boolean> deviceStateMap = new HashMap<>();

    public interface OnDeviceSwitchChangedListener {
        void onDeviceSwitchChanged(String topic, boolean isChecked);
    }

    public DeviceLightGateAdapter(List<Device> devices, OnDeviceSwitchChangedListener listener) {
        this.devices = devices;
        this.listener = listener;
        // Initialize device state map
        for (Device device : devices) {
            deviceStateMap.put(device.getMqttSubTopic(), false); // Default state
        }
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.itemdevicelightgate, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        Device device = devices.get(position);
        holder.deviceNameTextView.setText(device.getDeviceName());
        holder.deviceStatusTextView.setText("Status: " + (deviceStateMap.getOrDefault(device.getMqttSubTopic(), false) ? "ON" : "OFF"));

        holder.deviceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            listener.onDeviceSwitchChanged(device.getMqttSubTopic(), isChecked);
            // Optional: Update device state map if needed here
        });

        // Set switch state based on device state map
        holder.deviceSwitch.setChecked(deviceStateMap.getOrDefault(device.getMqttSubTopic(), false));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void updateDeviceState(String topic, boolean isOn) {
        deviceStateMap.put(topic, isOn);
        // Find the position of the device and notify adapter
        for (int i = 0; i < devices.size(); i++) {
            Device device = devices.get(i);
            if (device.getMqttSubTopic().equals(topic)) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceNameTextView;
        TextView deviceStatusTextView;
        Switch deviceSwitch;

        DeviceViewHolder(View itemView) {
            super(itemView);
            deviceNameTextView = itemView.findViewById(R.id.deviceNameTextView);
            deviceStatusTextView = itemView.findViewById(R.id.deviceStatusTextView);
            deviceSwitch = itemView.findViewById(R.id.deviceSwitch);
        }

    }

}
