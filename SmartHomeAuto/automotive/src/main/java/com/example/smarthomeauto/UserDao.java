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

    @Query("SELECT * FROM users WHERE Role != 'adminmaster'")
    List<User> getAllUsers();

    @Query("SELECT * FROM users WHERE UserID = :userId")
    User getUserById(int userId);

    @Query("SELECT * FROM users WHERE Username = :username")
    User getUserByUsername(String username);

    @Query("SELECT * FROM users WHERE Username = :username AND Password = :password")
    User authenticateUser(String username, String password);

    @Query("SELECT COUNT(*) FROM users WHERE Username = :username")
    int countUsersByUsername(String username);

    @Query("SELECT * FROM users WHERE Username = :username AND Password = :password")
    User getUserByCredentials(String username, String password);

    @Query("SELECT * FROM users WHERE ManagerUserId = :managerId")
    List<User> getUsersByManagerId(int managerId);

    @Query("SELECT * FROM users WHERE UserID = (SELECT ManagerUserId FROM users WHERE UserID = :userId)")
    User getManagerForUser(int userId);

    @Query("SELECT * FROM users " +
            "WHERE (:username IS NULL OR Username LIKE '%' || :username || '%') " +
            "AND (:role IS NULL OR Role = :role) " +
            "AND (:broker IS NULL OR EXISTS (SELECT 1 FROM broker " +
            "WHERE broker.BrokerID = users.UserBrokerID " +
            "AND (ClusterUrl || ':' || Port) LIKE :broker))")
    List<User> searchUsers(String username, String role, String broker);

    @Query("SELECT * FROM users " +
            "WHERE (:username IS NULL OR Username LIKE '%' || :username || '%') " +
            "AND (:role IS NULL OR Role = :role) " +
            "AND Role != 'admin' " +
            "AND Role != 'adminmaster' " +
            "AND (:broker IS NULL OR EXISTS (SELECT 1 FROM broker " +
            "WHERE broker.BrokerID = users.UserBrokerID " +
            "AND (ClusterUrl || ':' || Port) LIKE :broker))")
    List<User> searchUsersExcludingAdmin(String username, String role, String broker);

    @Query("SELECT * FROM users WHERE Role != 'admin'AND Role != 'adminmaster'")
    List<User> getAllUsersExcludingAdmin();


    @Query("SELECT * FROM users WHERE Role = 'guest' AND ManagerUserId = :managerId")
    List<User> getAllGuests(int managerId);

    @Query("SELECT * FROM users WHERE Role = :role " +
            "AND ((Role = 'user' AND (MqttUser IS NULL OR MqttPassword IS NULL OR UserBrokerID IS NULL)) " +
            "OR (Role = 'guest' AND (MqttUser IS NULL OR MqttPassword IS NULL OR UserBrokerID IS NULL OR ManagerUserId IS NULL)))")
    List<User> searchUsersWithNullFields( String role);

    @Query("SELECT * FROM users WHERE MqttUser IS NULL OR MqttPassword IS NULL OR UserBrokerID IS NULL OR ManagerUserId IS NULL")
    List<User> searchAllUsersWithNullFields();

    @Query("SELECT UserID FROM users WHERE Username = :username")
    int getUserIdByUsername(String username);

    @Query("SELECT COUNT(*) > 0 FROM users WHERE UserID = :userId AND (MqttUser IS NULL OR MqttPassword IS NULL OR UserBrokerID IS NULL)")
    boolean doesUserHaveNullFields(int userId);


    @Query("SELECT UserBrokerID FROM Users WHERE UserID = :userId")
    int getBrokerById(int userId);

    @Query("SELECT MqttUser FROM Users WHERE UserID = :userId")
    String getMqtttUsernameById(int userId);

    @Query("SELECT MqttPassword FROM Users WHERE UserID = :userId")
    String getMqtttpassordById(int userId);

    @Query("SELECT Role FROM Users WHERE UserID = :userId")
    String getRoleById(int userId);

}
