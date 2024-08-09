package com.example.smarthomeauto;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
        tableName = "user_devices",
        primaryKeys = {"PermissionUserID", "PermissionDeviceId"},
        foreignKeys = {
                @ForeignKey(entity = User.class, parentColumns = "UserID", childColumns = "PermissionUserID"),
                @ForeignKey(entity = Device.class, parentColumns = "DevicesID", childColumns = "PermissionDeviceId")
        }
)
public class UserDevice {
    public int PermissionUserID;
    public int PermissionDeviceId;
    public String permissions; // e.g., "read", "write", "control"

    public UserDevice() {
    }

    public UserDevice(int userId, int deviceId, String permissions) {
        this.PermissionUserID = userId;
        this.PermissionDeviceId = deviceId;
        this.permissions = permissions;
    }
    public int getPermissionDeviceId() {
        return PermissionDeviceId;
    }

    public String getPermissions() {
        return permissions;
    }
}
