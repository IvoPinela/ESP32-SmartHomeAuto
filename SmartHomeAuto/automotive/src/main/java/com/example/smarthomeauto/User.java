package com.example.smarthomeauto;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(
        tableName = "users",
        foreignKeys = {
                @ForeignKey(
                        entity = User.class,
                        parentColumns = "UserID",
                        childColumns = "ManagerUserId",
                        onDelete = ForeignKey.SET_NULL,
                        onUpdate = ForeignKey.NO_ACTION
                ),
                @ForeignKey(
                        entity = Broker.class,
                        parentColumns = "BrokerID",
                        childColumns = "UserBrokerID",
                        onDelete = ForeignKey.SET_NULL,
                        onUpdate = ForeignKey.NO_ACTION
                )
        }
)


public class User implements Serializable{
    @PrimaryKey(autoGenerate = true)
    public int UserID;

    @NonNull
    public String Username;

    @NonNull
    public String Password;

    @NonNull
    public String Role;

    public String MqttUser;
    public String MqttPassword;
    public Integer ManagerUserId;
    public Integer UserBrokerID;

    public User() {}

    public User(String username, String password, String role, String mqttUser, String mqttPassword, Integer managerUserId, Integer brokerID) {
        this.Username = username;
        this.Password = password;
        this.Role = role;
        this.MqttUser = mqttUser;
        this.MqttPassword = mqttPassword;
        this.ManagerUserId = managerUserId;
        this.UserBrokerID = brokerID;
    }
    @Override
    public String toString() {
        return Username;
    }
}
