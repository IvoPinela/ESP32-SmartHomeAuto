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

    @Query("SELECT * FROM devices WHERE name = :deviceName AND creatorUserId = :creatorUserId")
    Device getDeviceByNameAndUser(String deviceName, int creatorUserId);

    @Query("SELECT * FROM devices WHERE name = :deviceName AND creatorUserId = :creatorUserId AND id != :deviceId")
    Device getDeviceByNameAndUserExceptId(String deviceName, int creatorUserId, int deviceId);

    @Query("SELECT * FROM devices WHERE id = :deviceId")
    Device getDeviceById(int deviceId);

    @Query("SELECT * FROM devices WHERE name LIKE :deviceName")
    List<Device> getDevicesByName(String deviceName);

    @Query("SELECT * FROM devices WHERE deviceTypeId = :deviceTypeId")
    List<Device> getDevicesByType(int deviceTypeId);

    @Query("SELECT * FROM devices WHERE creatorUserId IN (SELECT id FROM users WHERE username LIKE '%' || :creatorName || '%') AND deviceTypeId = :deviceTypeId")
    List<Device> searchDevicesByCreatorNameAndType(String creatorName, Integer deviceTypeId);

    @Query("SELECT * FROM devices WHERE creatorUserId IN (SELECT id FROM users WHERE username LIKE '%' || :creatorName || '%')")
    List<Device> getDevicesByCreatorName(String creatorName);

    @Query("SELECT d.* " +
            "FROM devices d " +
            "INNER JOIN users u ON d.creatorUserId = u.id " +
            "INNER JOIN device_types dt ON d.deviceTypeId = dt.id " +
            "WHERE (:deviceName IS NULL OR d.name LIKE '%' || :deviceName || '%') " +
            "AND (:deviceTypeId IS NULL OR d.deviceTypeId = :deviceTypeId) " +
            "AND (:creatorName IS NULL OR u.username LIKE '%' || :creatorName || '%')")
    List<Device> searchDevices2(String deviceName, Integer deviceTypeId, String creatorName);
    @Query(
            "SELECT d.* " +
                    "FROM devices d " +
                    "INNER JOIN device_types dt ON d.deviceTypeId = dt.id " +
                    "WHERE (:deviceName IS NULL OR d.name LIKE '%' || :deviceName || '%') " +
                    "AND (:deviceTypeId IS NULL OR d.deviceTypeId = :deviceTypeId)"
    )
    List<Device> searchDevices(String deviceName, Integer deviceTypeId);

    @Query("SELECT * FROM devices " +
            "WHERE (mqttUser IS NULL OR mqttUser = '') " +
            "AND (mqttPassword IS NULL OR mqttPassword = '')")
    List<Device> searchDevicesWithMissingMQTT();

    @Query("SELECT * FROM devices WHERE " +
            "(:name IS NULL OR name LIKE '%' || :name || '%') " +
            "AND (:deviceTypeId IS NULL OR deviceTypeId = :deviceTypeId) " +
            "AND (:user IS NULL OR creatorUserId IN (SELECT id FROM users WHERE username LIKE '%' || :user || '%'))")
    List<Device> searchDevices(String name, Integer deviceTypeId, String user);

    @Query("SELECT * FROM devices WHERE " +
            "(:name IS NULL OR name LIKE '%' || :name || '%') " +
            "AND (:deviceTypeId IS NULL OR deviceTypeId = :deviceTypeId) " +
            "AND (:user IS NULL OR creatorUserId IN (SELECT id FROM users WHERE username LIKE '%' || :user || '%'))")
    List<Device> searchDevicesWithFilter(String name, Integer deviceTypeId, String user);



}
