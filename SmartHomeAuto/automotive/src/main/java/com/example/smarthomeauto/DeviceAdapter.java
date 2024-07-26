package com.example.smarthomeauto;

import android.app.Activity;
import android.content.Context;
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

    public DeviceAdapter(Context context, List<Device> devices) {
        super(context, 0, devices);
        // Inicialize o DeviceTypeDao
        deviceTypeDao = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "db_SmartHomeAuto").build().deviceTypeDao();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.itemdevice, parent, false);
        }

        Device device = getItem(position);

        TextView textViewName = convertView.findViewById(R.id.textViewName);

        TextView textViewTopic = convertView.findViewById(R.id.textViewTopic);

        TextView textViewServer = convertView.findViewById(R.id.textViewServer);

        TextView textViewPort = convertView.findViewById(R.id.textViewPort);

        TextView textViewDeviceType = convertView.findViewById(R.id.textViewDeviceType);


        // Popula os TextViews com as informações do dispositivo
        if (device != null) {
            textViewName.setText("Name:"+device.name);
            textViewTopic.setText("MQTT Topic: " + device.mqttTopic);
            textViewServer.setText("MQTT Server: " + device.mqttServer);
            textViewPort.setText("Port: " + device.mqttPort);


            // Obtenha a descrição do tipo de dispositivo
            new Thread(() -> {
                String deviceTypeName = deviceTypeDao.getDeviceTypeNameById(device.deviceTypeId);
                if (deviceTypeName == null) {
                    deviceTypeName = "Unknown";
                }
                String finalDeviceTypeName = deviceTypeName;
                ((Activity) getContext()).runOnUiThread(() -> {
                    textViewDeviceType.setText("Device Type: " + finalDeviceTypeName);
                });
            }).start();
        }

        // Define a cor de fundo com base na seleção
        if (position == selectedPosition) {
            convertView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorSelectedItem));
        } else {
            convertView.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.white));
        }

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
