package com.example.smarthomeauto;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BrokerDao {

    @Insert
    void insert(Broker broker);

    @Update
    void update(Broker broker);

    @Delete
    void delete(Broker broker);

    @Query("SELECT * FROM broker")
    List<Broker> getAllBrokers();

    @Query("SELECT * FROM broker WHERE PK_BrokerID = :brokerId")
    Broker getBrokerById(int brokerId);

    @Query("SELECT * FROM broker WHERE ClusterURL = :url")
    List<Broker> getBrokersByUrl(String url);

    @Query("SELECT * FROM broker WHERE PORT = :port")
    List<Broker> getBrokersByPort(int port);
}
