package com.example.smarthomeauto;

import android.content.Context;
import java.util.concurrent.Executors;

public class AppInitializer {

    public static void initializeDatabase(final Context context) {
        final AppDatabase db = AppDatabase.getDatabase(context);

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                UserDao userDao = db.userDao();
                DeviceTypeDao deviceTypeDao = db.deviceTypeDao();

                // Initialize users
                initializeUsers(userDao);

                // Initialize device types
                initializeDeviceTypes(deviceTypeDao);
            }
        });
    }

    private static void initializeUsers(UserDao userDao) {
        if (userDao.countUsersByUsername("admin") == 0) {
            User admin = new User();
            admin.username = "admin";
            admin.password = HashUtils.hashPassword("admin");
            admin.role = "admin";
            admin.mqttUser = null;
            admin.mqttPassword = null;
            admin.managerUserId = null;
            userDao.insert(admin);
        }

        if (userDao.countUsersByUsername("user") == 0) {
            User user = new User();
            user.username = "user";
            user.password = HashUtils.hashPassword("user");
            user.role = "user";
            user.mqttUser = "PublishTest";
            user.mqttPassword = "Publish123";
            user.managerUserId = null;
            userDao.insert(user);
        }
    }

    private static void initializeDeviceTypes(DeviceTypeDao deviceTypeDao) {
        insertDeviceTypeIfNotExists(deviceTypeDao, "gate","home/gate");
        insertDeviceTypeIfNotExists(deviceTypeDao, "light","home/light");
    }

    private static void insertDeviceTypeIfNotExists(DeviceTypeDao deviceTypeDao, String typeName,String topic) {
        if (deviceTypeDao.countDeviceTypesByName(typeName) == 0) {
            DeviceType deviceType = new DeviceType(typeName,topic);
            deviceTypeDao.insert(deviceType);
        }
    }
}
