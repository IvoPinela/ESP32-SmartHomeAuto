package com.example.smarthomeauto;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "device_types")
public class DeviceType {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name; // e.g., "light", "gate"
    public String mqttPrincipalTopic; // Novo campo para o tópico MQTT

    public DeviceType() {
    }

    public DeviceType(String name, String mqttTopic) {
        this.name = name;
        this.mqttPrincipalTopic = mqttTopic;
    }

    @Override
    public String toString() {
        return name;
    }
}
