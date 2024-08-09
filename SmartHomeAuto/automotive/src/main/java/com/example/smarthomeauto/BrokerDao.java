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

    @Query("SELECT * FROM broker WHERE BrokerID = :brokerId")
    Broker getBrokerById(int brokerId);

    @Query("SELECT COUNT(*) FROM broker")
    int countBrokers();

    @Query("SELECT BrokerID FROM broker WHERE ClusterUrl = :clusterURL")
    int getBrokerIdByUrl(String clusterURL);
    @Query("SELECT * FROM broker WHERE Port = :port")
    List<Broker> getBrokersByPort(int port);

    @Query("SELECT * FROM broker WHERE ClusterUrl LIKE '%' || :query || '%'")
    List<Broker> searchBrokers(String query);

    @Query("SELECT * FROM broker WHERE ClusterUrl = :clusterURL AND Port = :port")
    Broker getBrokerByUrlAndPort(String clusterURL, int port);

    @Query("SELECT * FROM broker WHERE ClusterUrl = :clusterURL AND Port = :port AND BrokerID != :id")
    Broker getBrokerByUrlAndPortExcludingId(String clusterURL, int port, int id);

    @Query("SELECT * FROM broker WHERE ClusterUrl = :url LIMIT 1")
    Broker getBrokerByUrl(String url);

    @Query("SELECT ClusterUrl FROM broker WHERE BrokerID = :brokerId")
    String getClusterURLById(int brokerId);
    @Query("SELECT Port FROM broker WHERE BrokerID = :brokerId")
    int getPORTById(int brokerId);
}
