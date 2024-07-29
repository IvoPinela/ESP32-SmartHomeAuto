package com.example.smarthomeauto;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserDeviceDao {
    @Insert
    void insert(UserDevice userDevice);

    @Query("SELECT * FROM devices INNER JOIN user_devices ON devices.id = user_devices.deviceId WHERE user_devices.userId = :userId")
    List<Device> getDevicesForUser(int userId);

    @Query("SELECT * FROM users INNER JOIN user_devices ON users.id = user_devices.userId WHERE user_devices.deviceId = :deviceId")
    List<User> getUsersForDevice(int deviceId);
}
