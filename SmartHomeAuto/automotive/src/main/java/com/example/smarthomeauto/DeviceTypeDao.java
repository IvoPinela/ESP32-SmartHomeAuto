package com.example.smarthomeauto;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface DeviceTypeDao {

    @Insert
    void insert(DeviceType deviceType);

    @Update
    void update(DeviceType deviceType);

    @Delete
    void delete(DeviceType deviceType);

    @Query("SELECT * FROM device_types")
    List<DeviceType> getAllDeviceTypes();

    @Query("SELECT * FROM device_types WHERE id = :typeId")
    DeviceType getDeviceTypeById(int typeId);

    @Query("SELECT * FROM device_types WHERE name LIKE :typeName")
    List<DeviceType> getDeviceTypesByName(String typeName);
}
