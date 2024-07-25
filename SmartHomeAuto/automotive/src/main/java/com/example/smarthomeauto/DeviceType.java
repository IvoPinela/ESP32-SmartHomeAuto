package com.example.smarthomeauto;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "device_types")
public class DeviceType {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name; // e.g., "light", "gate"

    // Construtor padrão
    public DeviceType() {
    }

    // Construtor completo
    public DeviceType(String name) {
        this.name = name;
    }
}
