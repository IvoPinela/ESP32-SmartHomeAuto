package com.example.smarthomeauto;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.content.Context;

@Database(entities = {User.class, Device.class, DeviceType.class, UserDevice.class, Broker.class}, version = 6, exportSchema = false)
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
                            .fallbackToDestructiveMigration()
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

            database.execSQL("DROP TABLE IF EXISTS `user_devices`");
            database.execSQL("DROP TABLE IF EXISTS `devices`");
            database.execSQL("DROP TABLE IF EXISTS `users`");
            database.execSQL("DROP TABLE IF EXISTS `broker`");
            database.execSQL("DROP TABLE IF EXISTS `device_types`");

            database.execSQL("CREATE TABLE IF NOT EXISTS `device_types` (" +
                    "`DeviceTypeID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`DeviceTypeName` TEXT)");

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

            database.execSQL("ALTER TABLE device_types ADD COLUMN MqttPrincipalTopic TEXT");
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Remover as tabelas existentes
            database.execSQL("DROP TABLE IF EXISTS `user_devices`");
            database.execSQL("DROP TABLE IF EXISTS `devices`");
            database.execSQL("DROP TABLE IF EXISTS `users`");
            database.execSQL("DROP TABLE IF EXISTS `broker`");
            database.execSQL("DROP TABLE IF EXISTS `device_types`");

            // Criar a tabela 'device_types'
            database.execSQL("CREATE TABLE IF NOT EXISTS `device_types` (" +
                    "`DeviceTypeID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL CHECK(`DeviceTypeID` <= 99), " +
                    "`DeviceTypeName` TEXT NOT NULL CHECK(LENGTH(`DeviceTypeName`) <= 60), " +
                    "`MqttPrincipalTopic` TEXT CHECK(LENGTH(`MqttPrincipalTopic`) <= 40))");

            // Criar a tabela 'devices'
            database.execSQL("CREATE TABLE IF NOT EXISTS `devices` (" +
                    "`DevicesID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL CHECK(`DevicesID` <= 9999999), " +
                    "`DeviceName` TEXT NOT NULL CHECK(LENGTH(`DeviceName`) <= 60), " +
                    "`MqttSubTopic` TEXT CHECK(LENGTH(`MqttSubTopic`) <= 60), " +
                    "`MqttUser` TEXT CHECK(LENGTH(`MqttUser`) <= 40), " +
                    "`MqttPassword` TEXT CHECK(LENGTH(`MqttPassword`) <= 40), " +
                    "`TypeId` INTEGER NOT NULL CHECK(`TypeId` <= 99), " +
                    "`CreatorUserId` INTEGER NOT NULL CHECK(`CreatorUserId` <= 999999), " +
                    "FOREIGN KEY(`TypeId`) REFERENCES device_types(`DeviceTypeID`) ON DELETE CASCADE, " +
                    "FOREIGN KEY(`CreatorUserId`) REFERENCES users(`UserID`) ON DELETE CASCADE)");

            // Criar a tabela 'user_devices'
            database.execSQL("CREATE TABLE IF NOT EXISTS `user_devices` (" +
                    "`PermissionUserID` INTEGER NOT NULL CHECK(`PermissionUserID` <= 999999), " +
                    "`PermissionDeviceId` INTEGER NOT NULL CHECK(`PermissionDeviceId` <= 9999999), " +
                    "`permissions` TEXT CHECK(LENGTH(`permissions`) <= 50), " +
                    "PRIMARY KEY(`PermissionUserID`, `PermissionDeviceId`), " +
                    "FOREIGN KEY(`PermissionUserID`) REFERENCES users(`UserID`) ON DELETE CASCADE, " +
                    "FOREIGN KEY(`PermissionDeviceId`) REFERENCES devices(`DevicesID`) ON DELETE CASCADE)");

            // Criar a tabela 'broker'
            database.execSQL("CREATE TABLE IF NOT EXISTS `broker` (" +
                    "`BrokerID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL CHECK(`BrokerID` <= 999999), " +
                    "`ClusterUrl` TEXT CHECK(LENGTH(`ClusterUrl`) <= 51), " +
                    "`Port` INTEGER CHECK(`Port` >= 0 AND `Port` <= 9999))");

            // Criar a tabela 'users'
            database.execSQL("CREATE TABLE IF NOT EXISTS `users` (" +
                    "`UserID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL CHECK(`UserID` <= 999999), " +
                    "`Username` TEXT NOT NULL CHECK(LENGTH(`Username`) <= 60), " +
                    "`Password` TEXT NOT NULL CHECK(LENGTH(`Password`) <= 60), " +
                    "`Role` TEXT NOT NULL CHECK(LENGTH(`Role`) <= 30), " +
                    "`MqttUser` TEXT CHECK(LENGTH(`MqttUser`) <= 40), " +
                    "`MqttPassword` TEXT CHECK(LENGTH(`MqttPassword`) <= 40), " +
                    "`ManagerUserId` INTEGER CHECK(`ManagerUserId` <= 999999), " +
                    "`UserBrokerID` INTEGER CHECK(`UserBrokerID` <= 999999), " +
                    "FOREIGN KEY(`ManagerUserId`) REFERENCES users(`UserID`) ON DELETE SET NULL, " +
                    "FOREIGN KEY(`UserBrokerID`) REFERENCES broker(`BrokerID`) ON DELETE SET NULL)");

            // Cria índices
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_users_ManagerUserId` ON `users`(`ManagerUserId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_users_UserBrokerID` ON `users`(`UserBrokerID`)");
        }
    };
}
