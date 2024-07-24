package com.example.smarthomeauto;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface DeviceDao {

    // Inserir um novo dispositivo
    @Insert
    void insert(Device device);

    // Atualizar um dispositivo existente
    @Update
    void update(Device device);

    // Deletar um dispositivo
    @Delete
    void delete(Device device);

    // Consultar todos os dispositivos
    @Query("SELECT * FROM devices")
    List<Device> getAllDevices();

    // Consultar um dispositivo pelo ID
    @Query("SELECT * FROM devices WHERE id = :deviceId")
    Device getDeviceById(int deviceId);

    // Consultar dispositivos pelo nome
    @Query("SELECT * FROM devices WHERE name LIKE :deviceName")
    List<Device> getDevicesByName(String deviceName);
}

