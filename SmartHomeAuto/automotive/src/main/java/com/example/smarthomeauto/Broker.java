package com.example.smarthomeauto;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "broker")
public class Broker implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int BrokerID;

    public String ClusterUrl;
    public int Port;

    public Broker() {
    }

    public Broker(String clusterURL, int port) {
        this.ClusterUrl = clusterURL;
        this.Port = port;
    }
}

