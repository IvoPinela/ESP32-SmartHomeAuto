package com.example.smarthomeauto;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.content.Context;

@Database(entities = {User.class, Device.class, DeviceType.class, UserDevice.class, Broker.class}, version = 5, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract DeviceDao deviceDao();
    public abstract DeviceTypeDao deviceTypeDao();
    public abstract UserDeviceDao userDeviceDao();
    public abstract BrokerDao brokerDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "db_SmartHomeAuto")

                            .addMigrations(
                                    MIGRATION_1_2,
                                    MIGRATION_2_3,
                                    MIGRATION_3_4,
                                    MIGRATION_4_5

                            )
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `device_types` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT)");

            database.execSQL("DROP TABLE IF EXISTS `devices`");

            database.execSQL("CREATE TABLE IF NOT EXISTS `devices` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT, " +
                    "`mqttTopic` TEXT, " +
                    "`mqttServer` TEXT, " +
                    "`mqttUser` TEXT, " +
                    "`mqttPassword` TEXT, " +
                    "`mqttPort` INTEGER NOT NULL DEFAULT 0, " +
                    "`deviceTypeId` INTEGER NOT NULL, " +
                    "`creatorUserId` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`deviceTypeId`) REFERENCES device_types(`id`) ON DELETE CASCADE, " +
                    "FOREIGN KEY(`creatorUserId`) REFERENCES users(`id`) ON DELETE CASCADE)");
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

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Remove tabelas antigas
            database.execSQL("DROP TABLE IF EXISTS `user_devices`");
            database.execSQL("DROP TABLE IF EXISTS `devices`");
            database.execSQL("DROP TABLE IF EXISTS `users`");
            database.execSQL("DROP TABLE IF EXISTS `broker`");
            database.execSQL("DROP TABLE IF EXISTS `device_types`");

            database.execSQL("CREATE TABLE IF NOT EXISTS `device_types` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `devices` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT, " +
                    "`mqttTopic` TEXT, " +
                    "`mqttUser` TEXT, " +
                    "`mqttPassword` TEXT, " +
                    "`deviceTypeId` INTEGER NOT NULL, " +
                    "`creatorUserId` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`deviceTypeId`) REFERENCES device_types(`id`) ON DELETE CASCADE, " +
                    "FOREIGN KEY(`creatorUserId`) REFERENCES users(`id`) ON DELETE CASCADE)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `user_devices` (" +
                    "`userId` INTEGER NOT NULL, " +
                    "`deviceId` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`userId`, `deviceId`), " +
                    "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON DELETE CASCADE, " +
                    "FOREIGN KEY(`deviceId`) REFERENCES `devices`(`id`) ON DELETE CASCADE)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `broker` (" +
                    "`PK_BrokerID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`ClusterURL` TEXT, " +
                    "`PORT` INTEGER)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `users` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`username` TEXT NOT NULL, " +
                    "`password` TEXT NOT NULL, " +
                    "`role` TEXT NOT NULL, " +
                    "`mqttUser` TEXT, " +
                    "`mqttPassword` TEXT, " +
                    "`managerUserId` INTEGER, " +
                    "`brokerID` INTEGER, " +
                    "FOREIGN KEY(`managerUserId`) REFERENCES users(`id`) ON DELETE SET NULL, " +
                    "FOREIGN KEY(`brokerID`) REFERENCES broker(`PK_BrokerID`) ON DELETE SET NULL)");

            // Cria índices
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_users_managerUserId` ON `users`(`managerUserId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_users_brokerID` ON `users`(`brokerID`)");
        }
    };
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {

            database.execSQL("ALTER TABLE device_types ADD COLUMN mqttPrincipalTopic TEXT");
        }
    };

}
