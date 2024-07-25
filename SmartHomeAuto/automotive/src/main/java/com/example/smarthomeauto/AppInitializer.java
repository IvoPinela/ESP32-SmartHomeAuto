package com.example.smarthomeauto;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.mindrot.jbcrypt.BCrypt;

import java.util.concurrent.Executors;

public class AppInitializer {

    public static void initializeDatabase(final Context context) {
        final AppDatabase db = AppDatabase.getDatabase(context);

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                UserDao userDao = db.userDao();

                if (userDao.countUsersByUsername("admin") == 0) {
                    User admin = new User();
                    admin.username = "admin";
                    admin.password = HashUtils.hashPassword("admin");
                    admin.role = "admin";
                    userDao.insert(admin);
                }

                if (userDao.countUsersByUsername("user") == 0) {
                    User user = new User();
                    user.username = "user";
                    user.password = HashUtils.hashPassword("user");
                    user.role = "user";
                    userDao.insert(user);
                }
            }
        });
    }
}

