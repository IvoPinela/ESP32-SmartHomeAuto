package com.example.smarthomeauto;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public interface UserDeviceDao {


    @Insert
    void insert(UserDevice userDevice);


    @Delete
    void delete(UserDevice userDevice);


    @Query("SELECT * FROM user_devices WHERE userId = :userId")
    List<UserDevice> getDevicesForUser(int userId);


    @Query("SELECT * FROM user_devices WHERE deviceId = :deviceId")
    List<UserDevice> getUsersForDevice(int deviceId);


    @Query("SELECT COUNT(*) FROM user_devices WHERE userId = :userId AND deviceId = :deviceId")
    int checkUserDeviceExists(int userId, int deviceId);
}

