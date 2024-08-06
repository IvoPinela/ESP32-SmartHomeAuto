package com.example.smarthomeauto;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(
        tableName = "devices",
        foreignKeys = {
                @ForeignKey(entity = DeviceType.class, parentColumns = "id", childColumns = "deviceTypeId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "creatorUserId", onDelete = ForeignKey.CASCADE)
        }
)
public class Device implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String mqttTopic;
    public String mqttUser;
    public String mqttPassword;
    public int deviceTypeId;
    public int creatorUserId;

    public Device() {
    }

    public Device(String name, String mqttTopic,  String mqttUser, String mqttPassword, int deviceTypeId, int creatorUserId) {
        this.name = name;
        this.mqttTopic = mqttTopic;

        this.mqttUser = mqttUser;
        this.mqttPassword = mqttPassword;

        this.deviceTypeId = deviceTypeId;
        this.creatorUserId = creatorUserId;
    }
    public String getMqttTopic() {
        return mqttTopic;
    }
    public String getName() {
        return name;
    }

    public int getId(){
        return id;
    }
}
