package com.example.smarthomeauto;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
        tableName = "user_devices",
        primaryKeys = {"userId", "deviceId"},
        foreignKeys = {
                @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Device.class, parentColumns = "id", childColumns = "deviceId", onDelete = ForeignKey.CASCADE)
        }
)
public class UserDevice {
    public int userId;
    public int deviceId;

    public UserDevice(int userId, int deviceId) {
        this.userId = userId;
        this.deviceId = deviceId;
    }
}
