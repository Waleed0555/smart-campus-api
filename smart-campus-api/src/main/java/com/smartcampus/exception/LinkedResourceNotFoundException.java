/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.exception;

public class LinkedResourceNotFoundException extends RuntimeException {

    private String field;
    private String value;

    public LinkedResourceNotFoundException(String field, String value) {
        super("Could not find " + field + " with value: " + value);
        this.field = field;
        this.value = value;
    }

    public String getField() { return field; }
    public String getValue() { return value; }

}