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

    @Query("SELECT * FROM device_types WHERE DeviceTypeID = :typeId")
    DeviceType getDeviceTypeById(int typeId);

    @Query("SELECT * FROM device_types WHERE DeviceTypeName LIKE :typeName")
    List<DeviceType> getDeviceTypesByName(String typeName);


    @Query("SELECT COUNT(*) FROM device_types WHERE DeviceTypeName = :name")
    int countDeviceTypesByName(String name);

    @Query("SELECT DeviceTypeName FROM device_types WHERE DeviceTypeID = :deviceTypeId")
    String getDeviceTypeNameById(int deviceTypeId);

    @Query("SELECT DeviceTypeID FROM device_types WHERE DeviceTypeName = :deviceTypeName")
    int getDeviceTypeIdByName(String deviceTypeName);

    @Query("SELECT MqttPrincipalTopic FROM device_types WHERE DeviceTypeID = :deviceTypeId")
    String getMqttPrincipalTopicById(int deviceTypeId);

    @Query("SELECT DeviceTypeID FROM device_types WHERE DeviceTypeName LIKE :typeName")
    int getIdByName(String typeName);



}
