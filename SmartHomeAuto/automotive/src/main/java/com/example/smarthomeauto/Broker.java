package com.example.smarthomeauto;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "broker")
public class Broker {
    @PrimaryKey(autoGenerate = true)
    public int PK_BrokerID;

    public String ClusterURL;
    public int PORT;

    public Broker() {
    }

    public Broker(String clusterURL, int port) {
        this.ClusterURL = clusterURL;
        this.PORT = port;
    }
}

