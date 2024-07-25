package com.example.smarthomeauto;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "devices",
        foreignKeys = @ForeignKey(
                entity = DeviceType.class,
                parentColumns = "id",
                childColumns = "deviceTypeId",
                onDelete = ForeignKey.CASCADE
        )
)
public class Device {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String mqttTopic;
    public String mqttServer;
    public String mqttUser;
    public String mqttPassword;
    public int mqttPort;


    public int deviceTypeId;


    public Device() {
    }


    public Device(String name, String mqttTopic, String mqttServer, String mqttUser,
                  String mqttPassword, int mqttPort, int deviceTypeId) {
        this.name = name;
        this.mqttTopic = mqttTopic;
        this.mqttServer = mqttServer;
        this.mqttUser = mqttUser;
        this.mqttPassword = mqttPassword;
        this.mqttPort = mqttPort;
        this.deviceTypeId = deviceTypeId;
    }
}
