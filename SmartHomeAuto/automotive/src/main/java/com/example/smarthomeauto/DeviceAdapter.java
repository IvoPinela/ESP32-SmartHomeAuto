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

public class DeviceAdapter extends ArrayAdapter<Device> {

    private int selectedPosition = -1;
    private DeviceTypeDao deviceTypeDao;
    private UserDao userDao;

    public DeviceAdapter(Context context, List<Device> devices) {
        super(context, 0, devices);

        AppDatabase database = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build();
        deviceTypeDao = database.deviceTypeDao();
        userDao = database.userDao();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.itemdevice, parent, false);
        }

        Device device = getItem(position);

        TextView textViewName = convertView.findViewById(R.id.textViewName);
        TextView textViewTopic = convertView.findViewById(R.id.textViewTopic);
        TextView textViewDeviceType = convertView.findViewById(R.id.textViewDeviceType);
        TextView textViewUser = convertView.findViewById(R.id.textViewUser);
        TextView textViewMqttUser = convertView.findViewById(R.id.textViewMqttUser);
        TextView textViewMqttPassword = convertView.findViewById(R.id.textViewMqttPassword);

        if (device != null) {
            textViewName.setText("Name: " + device.DeviceName);
            textViewTopic.setText("MQTT Topic: " + device.MqttSubTopic);
            textViewMqttUser.setText("MQTT User: " + device.MqttUser);
            textViewMqttPassword.setText("MQTT Password: " + device.MqttPassword);

            new Thread(() -> {
                String deviceTypeName = deviceTypeDao.getDeviceTypeNameById(device.TypeId);
                if (deviceTypeName == null) {
                    deviceTypeName = "Unknown";
                }
                String finalDeviceTypeName = deviceTypeName;
                ((Activity) getContext()).runOnUiThread(() -> {
                    textViewDeviceType.setText("Device Type: " + finalDeviceTypeName);
                });
            }).start();

            new Thread(() -> {
                User creatorUser = userDao.getUserById(device.CreatorUserId);
                final String creatorUserName = (creatorUser != null) ? creatorUser.Username : "Unknown";
                ((Activity) getContext()).runOnUiThread(() -> {
                    textViewUser.setText("Creator User: " + creatorUserName);
                });
            }).start();
        }

        // Definir cor de fundo
        int backgroundColor;
        if (position == selectedPosition) {
            backgroundColor = ContextCompat.getColor(getContext(), R.color.colorSelectedItem);
        } else if (device != null && (device.MqttUser == null || device.MqttUser.isEmpty() || device.MqttPassword == null || device.MqttPassword.isEmpty())) {
            backgroundColor = Color.YELLOW;
        } else {
            backgroundColor = ContextCompat.getColor(getContext(), android.R.color.white);
        }

        convertView.setBackgroundColor(backgroundColor);

        return convertView;
    }


    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    public void updateDeviceList(List<Device> newDevices) {
        clear();
        addAll(newDevices);
        notifyDataSetChanged();
    }

}
