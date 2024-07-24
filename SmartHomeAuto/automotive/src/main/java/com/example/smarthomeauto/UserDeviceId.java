package com.example.smarthomeauto;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

import java.util.Objects;

public class UserDeviceId {
    @NonNull
    @ColumnInfo(name = "userId")
    public int userId;

    @NonNull
    @ColumnInfo(name = "deviceId")
    public int deviceId;


    public UserDeviceId(int userId, int deviceId) {
        this.userId = userId;
        this.deviceId = deviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDeviceId that = (UserDeviceId) o;
        return userId == that.userId && deviceId == that.deviceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, deviceId);
    }

    @Override
    public String toString() {
        return "UserDeviceId{" +
                "userId=" + userId +
                ", deviceId=" + deviceId +
                '}';
    }
}
