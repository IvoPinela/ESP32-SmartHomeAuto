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
                DeviceDao deviceDao = db.deviceDao();
                BrokerDao brokerDao = db.brokerDao();
                UserDeviceDao userDeviceDao = db.userDeviceDao();

                initializeBrokers(brokerDao, db);
                initializeUsers(userDao, brokerDao);
                initializeDeviceTypes(deviceTypeDao);
                initializeDevices(deviceDao, deviceTypeDao, userDao, brokerDao);
                initializeUserDeviceAssociations(userDeviceDao, userDao, deviceDao);
            }
        });
    }

    private static void initializeBrokers(BrokerDao brokerDao, AppDatabase db) {
        String brokerUrl = "05e815044648452d9966e9b6701cb998.s1.eu.hivemq.cloud";

        Broker existingBroker = brokerDao.getBrokerByUrl(brokerUrl);

        if (existingBroker == null) {
            Broker broker1 = new Broker(brokerUrl, 8883);
            brokerDao.insert(broker1);
        }
    }


    private static void initializeUsers(UserDao userDao, BrokerDao brokerDao) {
        // Retrieve broker IDs
        int broker1Id = brokerDao.getBrokerIdByUrl("05e815044648452d9966e9b6701cb998.s1.eu.hivemq.cloud");


        if (userDao.countUsersByUsername("admin") == 0) {
            User admin = new User();
            admin.Username = "admin";
            admin.Password = HashUtils.hashPassword("admin");
            admin.Role = "admin";
            admin.MqttUser = null;
            admin.MqttPassword = null;
            admin.ManagerUserId = null;
            admin.UserBrokerID = null;
            userDao.insert(admin);
        }

        if (userDao.countUsersByUsername("master") == 0) {
            User master = new User();
            master.Username = "master";
            master.Password = HashUtils.hashPassword("master");
            master.Role = "adminmaster";
            master.MqttUser = null;
            master.MqttPassword = null;
            master.ManagerUserId = null;
            master.UserBrokerID = null;
            userDao.insert(master);
        }

        if (userDao.countUsersByUsername("user") == 0) {
            User user = new User();
            user.Username = "user";
            user.Password = HashUtils.hashPassword("user");
            user.Role = "user";
            user.MqttUser = "PublishTest";
            user.MqttPassword = "Publish123";
            user.ManagerUserId = null;
            user.UserBrokerID = broker1Id;
            userDao.insert(user);
        }

        if (userDao.countUsersByUsername("guest") == 0) {
            User guest = new User();
            guest.Username = "guest";
            guest.Password = HashUtils.hashPassword("guest");
            guest.Role = "guest";
            guest.MqttUser = "Guest";
            guest.MqttPassword = "Guest1234";
            guest.UserBrokerID = broker1Id;
            guest.ManagerUserId = userDao.getUserIdByUsername("user");
            userDao.insert(guest);
        }
    }

    private static void initializeDeviceTypes(DeviceTypeDao deviceTypeDao) {
        insertDeviceTypeIfNotExists(deviceTypeDao, "gate", "home/gate");
        insertDeviceTypeIfNotExists(deviceTypeDao, "light", "home/light");
    }

    private static void insertDeviceTypeIfNotExists(DeviceTypeDao deviceTypeDao, String typeName, String topic) {
        if (deviceTypeDao.countDeviceTypesByName(typeName) == 0) {
            DeviceType deviceType = new DeviceType(typeName, topic);
            deviceTypeDao.insert(deviceType);
        }
    }

    private static void initializeDevices(DeviceDao deviceDao, DeviceTypeDao deviceTypeDao, UserDao userDao, BrokerDao brokerDao) {
        // Recuperar IDs dos tipos de dispositivo
        Integer gateDeviceTypeId = deviceTypeDao.getDeviceTypeIdByName("gate");
        Integer lightDeviceTypeId = deviceTypeDao.getDeviceTypeIdByName("light");

        // Recuperar ID do usuário
        Integer userId = userDao.getUserIdByUsername("user");

        // Recuperar ID do broker
        Integer brokerId = brokerDao.getBrokerIdByUrl("05e815044648452d9966e9b6701cb998.s1.eu.hivemq.cloud");

        if (deviceDao.getDevicesByName("Front Gate").isEmpty()) {
            Device gateDevice = new Device();
            gateDevice.DeviceName = "Front Gate";
            gateDevice.TypeId = gateDeviceTypeId != null ? gateDeviceTypeId : 0;
            gateDevice.CreatorUserId = userId != null ? userId : 0;
            gateDevice.MqttSubTopic = "home/gate/frontgate";
            gateDevice.MqttUser = "Esp32Gate";
            gateDevice.MqttPassword = "Esp32Gate";
            deviceDao.insert(gateDevice);
        }

        if (deviceDao.getDevicesByName("Living Room").isEmpty()) {
            Device lightDevice = new Device();
            lightDevice.DeviceName = "Living Room";
            lightDevice.TypeId = lightDeviceTypeId != null ? lightDeviceTypeId : 0;
            lightDevice.CreatorUserId = userId != null ? userId : 0;
            lightDevice.MqttSubTopic = "home/light/livingroom";
            lightDevice.MqttUser = "Esp32Light";
            lightDevice.MqttPassword = "Esp32Light";
            deviceDao.insert(lightDevice);
        }

        if (deviceDao.getDevicesByName("Garage").isEmpty()) {
            Device lightDevice = new Device();
            lightDevice.DeviceName = "Garage";
            lightDevice.TypeId = lightDeviceTypeId != null ? lightDeviceTypeId : 0;
            lightDevice.CreatorUserId = userId != null ? userId : 0;
            lightDevice.MqttSubTopic = "home/light/garage";
            lightDevice.MqttUser = "";
            lightDevice.MqttPassword = "";
            deviceDao.insert(lightDevice);
        }
    }
    private static void initializeUserDeviceAssociations(UserDeviceDao userDeviceDao, UserDao userDao, DeviceDao deviceDao) {
        // Retrieve user ID for "guest"
        Integer guestId = userDao.getUserIdByUsername("guest");

        // Retrieve device IDs
        Integer frontGateDeviceId = deviceDao.getDeviceIdByName("Front Gate");
        Integer livingRoomDeviceId = deviceDao.getDeviceIdByName("Living Room");
        Integer garageDeviceId = deviceDao.getDeviceIdByName("Garage");

        if (guestId != null && frontGateDeviceId != null) {
            // Check if association already exists
            if (userDeviceDao.countUserDeviceAssociations(guestId, frontGateDeviceId) == 0) {
                // Associate guest with "Front Gate" with "read" permission
                UserDevice guestFrontGate = new UserDevice(guestId, frontGateDeviceId, "read");
                userDeviceDao.insert(guestFrontGate);
            }
        }

        if (guestId != null && livingRoomDeviceId != null) {
            // Check if association already exists
            if (userDeviceDao.countUserDeviceAssociations(guestId, livingRoomDeviceId) == 0) {
                // Associate guest with "Living Room" with "control" permission
                UserDevice guestLivingRoom = new UserDevice(guestId, livingRoomDeviceId, "control");
                userDeviceDao.insert(guestLivingRoom);
            }
        }
    }





}
