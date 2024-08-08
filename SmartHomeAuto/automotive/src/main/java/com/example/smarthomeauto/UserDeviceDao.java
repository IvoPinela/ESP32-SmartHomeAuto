package com.example.smarthomeauto;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface UserDeviceDao {
    @Insert
    void insert(UserDevice userDevice);

    @Delete
    void delete(UserDevice userDevice);

    @Update
    void update(UserDevice userDevice);

    @Query("SELECT * FROM user_devices WHERE userId = :userId")
    List<UserDevice> getDevicesByUserId(int userId);

    @Query("SELECT * FROM user_devices WHERE deviceId = :deviceId")
    List<UserDevice> getUsersByDeviceId(int deviceId);

    @Query("SELECT * FROM user_devices WHERE userId = :userId AND deviceId = :deviceId")
    UserDevice getUserDevice(int userId, int deviceId);

    @Query("SELECT * FROM devices INNER JOIN user_devices ON devices.id = user_devices.deviceId WHERE user_devices.userId = :userId")
    List<Device> getDevicesForUser(int userId);

    @Query("SELECT * FROM users INNER JOIN user_devices ON users.id = user_devices.userId WHERE user_devices.deviceId = :deviceId")
    List<User> getUsersForDevice(int deviceId);

    @Query("SELECT deviceId FROM user_devices WHERE userId = :userId AND permissions LIKE '%read%'")
    List<Integer> getReadableDeviceIdsByUserId(int userId);

    @Query("SELECT deviceId FROM user_devices WHERE userId = :guestId")
    List<Integer> getDeviceIdsForGuest(int guestId);


    @Query("SELECT * FROM user_devices ud " +
            "WHERE ud.userId = :guestId " +
            "AND (:deviceName IS NULL OR EXISTS (" +
            "SELECT 1 FROM devices d " +
            "WHERE d.id = ud.deviceId AND d.name LIKE '%' || :deviceName || '%')) " +
            "AND (:deviceTypeId IS NULL OR EXISTS (" +
            "SELECT 1 FROM devices d " +
            "WHERE d.id = ud.deviceId AND d.deviceTypeId = :deviceTypeId)) " +
            "AND (:permissions IS NULL OR ud.permissions = :permissions)")
    List<UserDevice> getFilteredUserDevices(int guestId, String deviceName, Integer deviceTypeId, String permissions);

    @Query("SELECT COUNT(*) FROM user_devices WHERE userId = :userId AND deviceId = :deviceId")
    int countUserDeviceAssociations(int userId, int deviceId);
}
