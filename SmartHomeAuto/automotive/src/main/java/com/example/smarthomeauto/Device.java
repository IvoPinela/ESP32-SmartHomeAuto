package com.example.smarthomeauto;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "devices")
public class Device {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String type; // "light", "gate"
}
