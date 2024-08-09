package com.example.smarthomeauto;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "device_types")
public class DeviceType {
    @PrimaryKey(autoGenerate = true)
    public int DeviceTypeID;

    public String DeviceTypeName; // e.g., "light", "gate"
    public String MqttPrincipalTopic;


    public DeviceType() {
    }

    public DeviceType(String name, String mqttTopic) {
        this.DeviceTypeName = name;
        this.MqttPrincipalTopic = mqttTopic;
    }

    @Override
    public String toString() {
        return DeviceTypeName;
    }

    public int getDeviceTypeID() {
        return DeviceTypeID;
    }

}
