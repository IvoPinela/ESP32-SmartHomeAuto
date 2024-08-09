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

    @Query("SELECT * FROM user_devices WHERE PermissionUserID = :userId")
    List<UserDevice> getDevicesByUserId(int userId);

    @Query("SELECT * FROM user_devices WHERE PermissionDeviceId = :deviceId")
    List<UserDevice> getUsersByDeviceId(int deviceId);

    @Query("SELECT * FROM user_devices WHERE PermissionUserID = :userId AND PermissionDeviceId = :deviceId")
    UserDevice getUserDevice(int userId, int deviceId);

    @Query("SELECT * FROM devices INNER JOIN user_devices ON devices.DevicesID = user_devices.PermissionDeviceId WHERE user_devices.PermissionUserID = :userId")
    List<Device> getDevicesForUser(int userId);

    @Query("SELECT * FROM users INNER JOIN user_devices ON users.UserID = user_devices.PermissionUserID WHERE user_devices.PermissionDeviceId = :deviceId")
    List<User> getUsersForDevice(int deviceId);

    @Query("SELECT PermissionDeviceId FROM user_devices WHERE PermissionUserID = :userId")
    List<Integer> getReadableDeviceIdsByUserId(int userId);

    @Query("SELECT PermissionDeviceId FROM user_devices WHERE PermissionUserID = :guestId")
    List<Integer> getDeviceIdsForGuest(int guestId);


    @Query("SELECT * FROM user_devices ud " +
            "WHERE ud.PermissionUserID = :guestId " +
            "AND (:deviceName IS NULL OR EXISTS (" +
            "SELECT 1 FROM devices d " +
            "WHERE d.DevicesID = ud.PermissionDeviceId AND d.DeviceName LIKE '%' || :deviceName || '%')) " +
            "AND (:deviceTypeId IS NULL OR EXISTS (" +
            "SELECT 1 FROM devices d " +
            "WHERE d.DevicesID = ud.PermissionDeviceId AND d.TypeId = :deviceTypeId)) " +
            "AND (:permissions IS NULL OR ud.permissions = :permissions)")
    List<UserDevice> getFilteredUserDevices(int guestId, String deviceName, Integer deviceTypeId, String permissions);

    @Query("SELECT COUNT(*) FROM user_devices WHERE PermissionUserID = :userId AND PermissionDeviceId = :deviceId")
    int countUserDeviceAssociations(int userId, int deviceId);
}
