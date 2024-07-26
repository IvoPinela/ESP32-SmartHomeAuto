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

    @Query("SELECT * FROM devices")
    List<Device> getAllDevices();

    @Query("SELECT * FROM devices WHERE id = :deviceId")
    Device getDeviceById(int deviceId);

    @Query("SELECT * FROM devices WHERE name LIKE :deviceName")
    List<Device> getDevicesByName(String deviceName);

    @Query("SELECT * FROM devices WHERE deviceTypeId = :deviceTypeId")
    List<Device> getDevicesByType(int deviceTypeId);


    @Query(
            "SELECT d.* " +
                    "FROM devices d " +
                    "INNER JOIN device_types dt ON d.deviceTypeId = dt.id " +
                    "WHERE (:deviceName IS NULL OR d.name LIKE '%' || :deviceName || '%') " +
                    "AND (:deviceTypeId IS NULL OR d.deviceTypeId = :deviceTypeId)"
    )
    List<Device> searchDevices(String deviceName, Integer deviceTypeId);
}
