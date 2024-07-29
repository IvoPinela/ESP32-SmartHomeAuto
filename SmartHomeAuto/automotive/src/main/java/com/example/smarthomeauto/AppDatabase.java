package com.example.smarthomeauto;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {User.class, Device.class, DeviceType.class,UserDevice.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract DeviceDao deviceDao();
    public abstract DeviceTypeDao deviceTypeDao();
    public abstract UserDeviceDao userDeviceDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "db_SmartHomeAuto")
                            .addMigrations(MIGRATION_1_2,MIGRATION_2_3)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Cria a tabela device_types se não existir
            database.execSQL("CREATE TABLE IF NOT EXISTS `device_types` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT)");

            database.execSQL("DROP TABLE IF EXISTS `devices`");

            database.execSQL("CREATE TABLE IF NOT EXISTS `devices` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`mqttUser` TEXT, " +
                    "`deviceTypeId` INTEGER NOT NULL, " +
                    "`name` TEXT, " +
                    "`mqttTopic` TEXT, " +
                    "`mqttServer` TEXT, " +
                    "`mqttPassword` TEXT, " +
                    "`mqttPort` INTEGER NOT NULL DEFAULT 0, " + // Ensure NOT NULL constraint
                    "FOREIGN KEY(`deviceTypeId`) REFERENCES device_types(`id`) ON DELETE CASCADE ON UPDATE NO ACTION)"); // Match the expected foreign key constraints
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `user_devices` (" +
                    "`userId` INTEGER NOT NULL, " +
                    "`deviceId` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`userId`, `deviceId`), " +
                    "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON DELETE CASCADE, " +
                    "FOREIGN KEY(`deviceId`) REFERENCES `devices`(`id`) ON DELETE CASCADE)");
        }
    };

}
