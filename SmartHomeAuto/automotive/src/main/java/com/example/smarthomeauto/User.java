package com.example.smarthomeauto;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.List;

@Entity(
        tableName = "users",
        foreignKeys = {
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "id",
                        childColumns = "managerUserId",
                        onDelete = ForeignKey.SET_NULL,
                        onUpdate = ForeignKey.NO_ACTION
                ),
                @ForeignKey(
                        entity = Broker.class,
                        parentColumns = "PK_BrokerID",
                        childColumns = "brokerID",
                        onDelete = ForeignKey.SET_NULL,
                        onUpdate = ForeignKey.NO_ACTION
                )
        }
)


public class User implements Serializable{
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String username;

    @NonNull
    public String password;

    @NonNull
    public String role;

    public String mqttUser;
    public String mqttPassword;
    public Integer managerUserId;
    public Integer brokerID;

    public User() {}

    public User(String username, String password, String role, String mqttUser, String mqttPassword, Integer managerUserId, Integer brokerID) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.mqttUser = mqttUser;
        this.mqttPassword = mqttPassword;
        this.managerUserId = managerUserId;
        this.brokerID = brokerID;
    }
    @Override
    public String toString() {
        return username;
    }
}
