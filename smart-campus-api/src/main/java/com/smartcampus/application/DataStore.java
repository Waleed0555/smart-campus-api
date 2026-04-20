/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.application;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// this class holds all the data in memory since we dont have a database
// i made it a singleton so everything shares the same data
public class DataStore {

    private static DataStore instance;

    private Map<String, Room> rooms = new HashMap<>();
    private Map<String, Sensor> sensors = new HashMap<>();
    private Map<String, List<SensorReading>> sensorReadings = new HashMap<>();

    private DataStore() {
        loadSampleData();
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public Map<String, Room> getRooms() {
        return rooms;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public List<SensorReading> getReadingsForSensor(String sensorId) {
        if (sensorReadings.get(sensorId) == null) {
            sensorReadings.put(sensorId, new ArrayList<>());
        }
            return sensorReadings.get(sensorId);
    }        

    // just adding some starting data so the api isnt empty when testing
    private void loadSampleData() {

        Room r1 = new Room("CG-104", "Computer Lab CG104", 35);
        Room r2 = new Room("CG-105", "Computer Lab CG105", 30);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);

        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE", 22.1, "CG-104");
        Sensor s2 = new Sensor("CO2-001", "CO2", "ACTIVE", 398.0, "CG-104");
        Sensor s3 = new Sensor("OCC-001", "Occupancy", "MAINTENANCE", 0.0, "CG-105");
        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        r1.getSensorIds().add("TEMP-001");
        r1.getSensorIds().add("CO2-001");
        r2.getSensorIds().add("OCC-001");

        // add a few readings to TEMP-001 so history isnt empty
        List<SensorReading> readings = getReadingsForSensor("TEMP-001");
        readings.add(new SensorReading(UUID.randomUUID().toString(), System.currentTimeMillis() - 120000, 21.4));
        readings.add(new SensorReading(UUID.randomUUID().toString(), System.currentTimeMillis() - 60000, 21.8));
        readings.add(new SensorReading(UUID.randomUUID().toString(), System.currentTimeMillis(), 22.1));
    }

}