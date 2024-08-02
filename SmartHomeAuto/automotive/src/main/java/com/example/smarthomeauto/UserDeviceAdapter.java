package com.example.smarthomeauto;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

public class UserDeviceAdapter extends ArrayAdapter<Device> {

    private int selectedPosition = -1;
    private DeviceTypeDao deviceTypeDao;

    public UserDeviceAdapter(Context context, List<Device> devices, DeviceTypeDao deviceTypeDao) {
        super(context, 0, devices);
        this.deviceTypeDao = deviceTypeDao; // Inicialize o DAO
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.itemdevice_user, parent, false);
        }

        Device device = getItem(position);

        TextView textViewName = convertView.findViewById(R.id.textViewName);
        TextView textViewDeviceType = convertView.findViewById(R.id.textViewDeviceType);

        if (device != null) {
            textViewName.setText("Name: " + device.name);

            if (deviceTypeDao != null) {
                new Thread(() -> {
                    String deviceTypeName = deviceTypeDao.getDeviceTypeNameById(device.deviceTypeId);
                    ((Activity) getContext()).runOnUiThread(() -> {
                        textViewDeviceType.setText("Device Type: " + deviceTypeName);
                    });
                }).start();
            } else {
                textViewDeviceType.setText("Device Type: Unknown");
            }
        }

        int backgroundColor = position == selectedPosition ?
                ContextCompat.getColor(getContext(), R.color.colorSelectedItem) :
                ContextCompat.getColor(getContext(), android.R.color.white);

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
