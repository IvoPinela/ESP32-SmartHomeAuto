package com.example.smarthomeauto;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Embedded;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(tableName = "user_devices",
        primaryKeys = {"userId", "deviceId"},
        foreignKeys = {
                @ForeignKey(entity = User.class,
                        parentColumns = "id",
                        childColumns = "userId"),
                @ForeignKey(entity = Device.class,
                        parentColumns = "id",
                        childColumns = "deviceId")
        },
        indices = {@Index("userId"), @Index("deviceId")}
)
public class UserDevice {
    @PrimaryKey
    @Embedded
    public UserDeviceId id;

    public UserDevice(int userId, int deviceId) {
        this.id = new UserDeviceId(userId, deviceId);
    }
}

