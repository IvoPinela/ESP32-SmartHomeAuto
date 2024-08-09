package com.example.smarthomeauto;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeviceUserAdapter extends ArrayAdapter<UserDevice> {
    private int selectedPosition = -1;
    private DeviceTypeDao deviceTypeDao;
    private DeviceDao deviceDao;
    private ExecutorService executorService;

    public DeviceUserAdapter(Context context, List<UserDevice> userDevices, DeviceTypeDao deviceTypeDao, DeviceDao deviceDao) {
        super(context, 0, userDevices);
        this.deviceTypeDao = deviceTypeDao;
        this.deviceDao = deviceDao;
        // Initialize the ExecutorService with a fixed thread pool
        this.executorService = Executors.newFixedThreadPool(4);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.itemdeviceuser, parent, false);
        }

        UserDevice userDevice = getItem(position);

        TextView textViewName = convertView.findViewById(R.id.textViewName);
        TextView textViewDeviceType = convertView.findViewById(R.id.textViewDeviceType);
        TextView textViewPermission = convertView.findViewById(R.id.textViewPermission);

        if (userDevice != null) {
            executorService.execute(() -> {
                Device device = deviceDao.getDeviceById(userDevice.getPermissionDeviceId());
                String deviceTypeName = deviceTypeDao.getDeviceTypeNameById(device.getTypeId());

                ((Activity) getContext()).runOnUiThread(() -> {
                    textViewName.setText("Name: " + device.getDeviceName());
                    textViewDeviceType.setText("Device Type: " + deviceTypeName);
                    textViewPermission.setText("Permission: " + userDevice.getPermissions());
                });
            });
        }

        int backgroundColor = position == selectedPosition ?
                ContextCompat.getColor(getContext(), R.color.colorSelectedItem) :
                ContextCompat.getColor(getContext(), android.R.color.white);
        convertView.setBackgroundColor(backgroundColor);

        Log.d("DeviceUserAdapter", "Item position: " + position + ", Selected position: " + selectedPosition);

        return convertView;
    }


    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    public void updateDeviceList(List<UserDevice> newDevices) {
        clear();
        addAll(newDevices);
        notifyDataSetChanged();
    }

    // Clean up the executor service when it's no longer needed
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
