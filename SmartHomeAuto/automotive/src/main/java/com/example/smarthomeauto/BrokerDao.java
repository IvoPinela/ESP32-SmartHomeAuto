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

    @Query("SELECT COUNT(*) FROM broker")
    int countBrokers();

    @Query("SELECT PK_BrokerID FROM broker WHERE ClusterURL = :clusterURL")
    int getBrokerIdByUrl(String clusterURL);
    @Query("SELECT * FROM broker WHERE PORT = :port")
    List<Broker> getBrokersByPort(int port);

    @Query("SELECT * FROM broker WHERE ClusterURL LIKE '%' || :query || '%'")
    List<Broker> searchBrokers(String query);

    @Query("SELECT * FROM broker WHERE ClusterURL = :clusterURL AND PORT = :port")
    Broker getBrokerByUrlAndPort(String clusterURL, int port);

    @Query("SELECT * FROM broker WHERE ClusterURL = :clusterURL AND PORT = :port AND PK_BrokerID != :id")
    Broker getBrokerByUrlAndPortExcludingId(String clusterURL, int port, int id);

    @Query("SELECT * FROM broker WHERE ClusterURL = :url LIMIT 1")
    Broker getBrokerByUrl(String url);


}
