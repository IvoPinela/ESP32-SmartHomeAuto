package com.example.smarthomeauto;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(
        tableName = "devices",
        foreignKeys = {
                @ForeignKey(entity = DeviceType.class, parentColumns = "DeviceTypeID", childColumns = "TypeId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class, parentColumns = "UserID", childColumns = "CreatorUserId", onDelete = ForeignKey.CASCADE)
        }
)
public class Device implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int DevicesID;

    public String DeviceName;
    public String MqttSubTopic;
    public String MqttUser;
    public String MqttPassword;
    public int TypeId;
    public int CreatorUserId;

    public Device() {
    }

    public Device(String name, String mqttTopic,  String mqttUser, String mqttPassword, int deviceTypeId, int creatorUserId) {
        this.DeviceName = name;
        this.MqttSubTopic = mqttTopic;

        this.MqttUser = mqttUser;
        this.MqttPassword = mqttPassword;

        this.TypeId = deviceTypeId;
        this.CreatorUserId = creatorUserId;
    }
    public String getMqttSubTopic() {
        return MqttSubTopic;
    }
    public String getDeviceName() {
        return DeviceName;
    }

    public int getDevicesID(){
        return DevicesID;
    }
    public int getTypeId() {
        return TypeId;
    }

}
