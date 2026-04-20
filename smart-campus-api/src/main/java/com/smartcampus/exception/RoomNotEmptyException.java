/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.exception;

public class RoomNotEmptyException extends RuntimeException {

    private String roomId;

    public RoomNotEmptyException(String roomId) {
        super("Cannot delete room " + roomId + " sensors in it");
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }

}