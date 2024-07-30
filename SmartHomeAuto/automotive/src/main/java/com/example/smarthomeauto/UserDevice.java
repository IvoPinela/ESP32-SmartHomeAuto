package com.example.smarthomeauto;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
        tableName = "user_devices",
        primaryKeys = {"userId", "deviceId"},
        foreignKeys = {
                @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "userId"),
                @ForeignKey(entity = Device.class, parentColumns = "id", childColumns = "deviceId")
        }
)
public class UserDevice {
    public int userId;
    public int deviceId;
    public String permissions; // e.g., "read", "write", "control"

    public UserDevice(int userId, int deviceId, String permissions) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.permissions = permissions;
    }
}
