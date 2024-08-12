package com.example.smarthomeauto;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface DeviceDao {

    @Insert
    void insert(Device device);

    @Update
    void update(Device device);

    @Delete
    void delete(Device device);

    @Query("SELECT * FROM devices ")
    List<Device> getAllDevices();

    @Query("SELECT * FROM devices WHERE DeviceName = :deviceName AND CreatorUserId = :creatorUserId")
    Device getDeviceByNameAndUser(String deviceName, int creatorUserId);

    @Query("SELECT * FROM devices WHERE DeviceName = :deviceName AND CreatorUserId = :creatorUserId AND DevicesID != :deviceId")
    Device getDeviceByNameAndUserExceptId(String deviceName, int creatorUserId, int deviceId);

    @Query("SELECT * FROM devices WHERE DevicesID = :deviceId")
    Device getDeviceById(int deviceId);

    @Query("SELECT * FROM devices WHERE DeviceName LIKE :deviceName")
    List<Device> getDevicesByName(String deviceName);

    @Query("SELECT * FROM devices WHERE TypeId = :deviceTypeId")
    List<Device> getDevicesByType(int deviceTypeId);

    @Query("SELECT * FROM devices WHERE CreatorUserId IN (SELECT DevicesID FROM users WHERE Username LIKE '%' || :creatorName || '%') AND TypeId = :deviceTypeId")
    List<Device> searchDevicesByCreatorNameAndType(String creatorName, Integer deviceTypeId);

    @Query("SELECT * FROM devices WHERE CreatorUserId IN (SELECT DevicesID FROM users WHERE Username LIKE '%' || :creatorName || '%')")
    List<Device> getDevicesByCreatorName(String creatorName);

    @Query("SELECT d.* " +
            "FROM devices d " +
            "INNER JOIN users u ON d.CreatorUserId = u.UserID " +
            "INNER JOIN device_types dt ON d.TypeId = dt.DeviceTypeID " +
            "WHERE (:deviceName IS NULL OR d.DeviceName LIKE '%' || :deviceName || '%') " +
            "AND (:deviceTypeId IS NULL OR d.TypeId = :deviceTypeId) " +
            "AND (:creatorName IS NULL OR u.Username LIKE '%' || :creatorName || '%')")
    List<Device> searchDevices2(String deviceName, Integer deviceTypeId, String creatorName);
    @Query(
            "SELECT d.* " +
                    "FROM devices d " +
                    "INNER JOIN device_types dt ON d.TypeId = dt.DeviceTypeID " +
                    "WHERE (:deviceName IS NULL OR d.DeviceName LIKE '%' || :deviceName || '%') " +
                    "AND (:deviceTypeId IS NULL OR d.TypeId = :deviceTypeId)"
    )
    List<Device> searchDevices(String deviceName, Integer deviceTypeId);

    @Query("SELECT * FROM devices " +
            "WHERE (MqttUser IS NULL OR MqttUser = '') " +
            "AND (MqttPassword IS NULL OR MqttPassword = '')")
    List<Device> searchDevicesWithMissingMQTT();

    @Query("SELECT * FROM devices WHERE " +
            "(:name IS NULL OR DeviceName LIKE '%' || :name || '%') " +
            "AND (:deviceTypeId IS NULL OR TypeId = :deviceTypeId) " +
            "AND (:user IS NULL OR CreatorUserId IN (SELECT DevicesID FROM users WHERE Username LIKE '%' || :user || '%'))")
    List<Device> searchDevices(String name, Integer deviceTypeId, String user);

    @Query("SELECT * FROM devices WHERE " +
            "(:name IS NULL OR DeviceName LIKE '%' || :name || '%') " +
            "AND (:deviceTypeId IS NULL OR TypeId = :deviceTypeId) " +
            "AND (:user IS NULL OR CreatorUserId IN (SELECT DevicesID FROM users WHERE Username LIKE '%' || :user || '%'))")
    List<Device> searchDevicesWithFilter(String name, Integer deviceTypeId, String user);

    @Query("SELECT * FROM devices WHERE CreatorUserId = :creatorUserId AND DeviceName LIKE '%' || :name || '%' AND (:deviceTypeId IS NULL OR TypeId = :deviceTypeId)")
    List<Device> searchDevicesByCreatorIdAndFilters(int creatorUserId, String name, Integer deviceTypeId);

    @Query("SELECT * FROM devices WHERE CreatorUserId = :userId")
    List<Device> getDevicesByUserId(int userId);

    @Query("SELECT * FROM devices WHERE TypeId = :deviceTypeId AND CreatorUserId = :userId")
    List<Device> getDevicesByTypeAndUser(int deviceTypeId, int userId);

    @Query("SELECT MqttSubTopic FROM devices WHERE DevicesID = :deviceId")
    String getTopicById(int deviceId);


    @Query("SELECT DevicesID FROM devices WHERE MqttSubTopic = :topic")
    int getDeviceIdByTopic(String topic);


    @Query("SELECT DevicesID FROM devices WHERE DeviceName = :Name")
    int   getDeviceIdByName(String Name);

    @Query("SELECT * FROM devices WHERE DevicesID IN (:deviceIds)")
    List<Device> getDevicesByIds(List<Integer> deviceIds);

    @Query("SELECT * FROM devices WHERE CreatorUserId = :userId")
    List<Device> getAllDevicesForUser(int userId);

    @Query("SELECT * FROM devices WHERE CreatorUserId = :userId AND DevicesID NOT IN (SELECT PermissionDeviceId FROM user_devices WHERE PermissionUserID = :guestId)")
    List<Device> getAvailableDevicesForUserExcludingGuest(int userId, int guestId);


    @Query("SELECT * FROM devices WHERE CreatorUserId = :creatorUserId AND DeviceName LIKE '%' || :name || '%' AND (:deviceTypeId IS NULL OR TypeId = :deviceTypeId)AND DevicesID NOT IN (SELECT PermissionDeviceId FROM user_devices WHERE PermissionUserID = :guestId)")
    List<Device> searchDevicesByCreatorIdAndFilters2(int creatorUserId, String name, Integer deviceTypeId,int guestId);

    @Query("SELECT DeviceName FROM devices WHERE DevicesID IN (:deviceId)")
    String getDeviceNameById(int deviceId);

    @Query("SELECT d.* FROM devices d " +
            "INNER JOIN users u ON d.CreatorUserId = u.UserID " +
            "WHERE (:name IS NULL OR d.DeviceName LIKE '%' || :name || '%') " +
            "AND (:deviceTypeId IS NULL OR d.TypeId = :deviceTypeId) " +
            "AND (:username IS NULL OR u.Username LIKE '%' || :username || '%') " +
            "AND (:filterMissingMQTT = 0 OR (d.MqttUser IS NULL OR d.MqttUser = '') " +
            "AND (d.MqttPassword IS NULL OR d.MqttPassword = ''))")
    List<Device> searchDevices(String name, Integer deviceTypeId, String username, boolean filterMissingMQTT);

    @Query("SELECT COUNT(*) FROM user_devices WHERE PermissionDeviceId = :deviceId")
    int countPermissionsForDevice(int deviceId);

}
