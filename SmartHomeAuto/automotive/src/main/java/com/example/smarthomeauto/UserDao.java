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

    @Query("SELECT * FROM users")
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

    // Método para buscar usuários que gerencia
    @Query("SELECT * FROM users WHERE managerUserId = :managerId")
    List<User> getUsersByManagerId(int managerId);

    // Método para buscar o gerente de um usuário
    @Query("SELECT * FROM users WHERE id = (SELECT managerUserId FROM users WHERE id = :userId)")
    User getManagerForUser(int userId);
}
