package com.example.smarthomeauto;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface UserDao {
    @Insert
    void insert(User user);

    @Update
    void update(User user);

    @Delete
    void delete(User user);

    @Query("SELECT * FROM users WHERE role != 'adminmaster'")
    List<User> getAllUsers();

    @Query("SELECT * FROM users WHERE id = :userId")
    User getUserById(int userId);

    @Query("SELECT * FROM users WHERE username = :username")
    User getUserByUsername(String username);

    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    User authenticateUser(String username, String password);

    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    int countUsersByUsername(String username);

    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    User getUserByCredentials(String username, String password);

    @Query("SELECT * FROM users WHERE managerUserId = :managerId")
    List<User> getUsersByManagerId(int managerId);

    @Query("SELECT * FROM users WHERE id = (SELECT managerUserId FROM users WHERE id = :userId)")
    User getManagerForUser(int userId);

    @Query("SELECT * FROM users " +
            "WHERE (:username IS NULL OR username LIKE '%' || :username || '%') " +
            "AND (:role IS NULL OR role = :role) " +
            "AND (:broker IS NULL OR EXISTS (SELECT 1 FROM broker " +
            "WHERE broker.PK_BrokerID = users.brokerID " +
            "AND (ClusterURL || ':' || PORT) LIKE :broker))")
    List<User> searchUsers(String username, String role, String broker);

    @Query("SELECT * FROM users " +
            "WHERE (:username IS NULL OR username LIKE '%' || :username || '%') " +
            "AND (:role IS NULL OR role = :role) " +
            "AND role != 'admin' " +
            "AND role != 'adminmaster' " +
            "AND (:broker IS NULL OR EXISTS (SELECT 1 FROM broker " +
            "WHERE broker.PK_BrokerID = users.brokerID " +
            "AND (ClusterURL || ':' || PORT) LIKE :broker))")
    List<User> searchUsersExcludingAdmin(String username, String role, String broker);

    @Query("SELECT * FROM users WHERE role != 'admin'AND role != 'adminmaster'")
    List<User> getAllUsersExcludingAdmin();


    @Query("SELECT * FROM users WHERE role = :role " +
            "AND ((role = 'user' AND (mqttUser IS NULL OR mqttPassword IS NULL OR brokerID IS NULL)) " +
            "OR (role = 'guest' AND (mqttUser IS NULL OR mqttPassword IS NULL OR brokerID IS NULL OR managerUserId IS NULL)))")
    List<User> searchUsersWithNullFields( String role);

    @Query("SELECT * FROM users WHERE mqttUser IS NULL OR mqttPassword IS NULL OR brokerID IS NULL OR managerUserId IS NULL")
    List<User> searchAllUsersWithNullFields();






}
