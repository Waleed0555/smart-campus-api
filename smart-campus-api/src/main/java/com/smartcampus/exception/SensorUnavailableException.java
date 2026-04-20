/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.exception;

public class SensorUnavailableException extends RuntimeException {

    private String sensorId;
    private String status;

    public SensorUnavailableException(String sensorId, String status) {
        super("Sensor " + sensorId + " cannot accept readings, current status is: " + status);
        this.sensorId = sensorId;
        this.status = status;
    }

    public String getSensorId() { return sensorId; }
    public String getStatus() { return status; }

}