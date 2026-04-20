/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.resource;

import com.smartcampus.application.DataStore;
import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private DataStore store = DataStore.getInstance();

    @GET
    public Response getSensors(@QueryParam("type") String type) {

        List<Sensor> result = new ArrayList<>(store.getSensors().values());
        

        // filter by type if the query param was given
        if (type != null && !type.isEmpty()) {
            List<Sensor> filtered = new ArrayList<>();
            for (Sensor s : result) {
                if (s.getType().equalsIgnoreCase(type)) {
                    filtered.add(s);
                }
            }
            return Response.ok(filtered).build();
        }

        return Response.ok(result).build();
    }

    @POST
    public Response addSensor(Sensor sensor) {

        // check the room actually exists first
        String rid = sensor.getRoomId();
        if (rid == null || !store.getRooms().containsKey(rid)) {
            throw new LinkedResourceNotFoundException("roomId", rid);
        }

        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            Map<String, String> e = new HashMap<>();
            e.put("error", "Sensor id cannot be empty");
            return Response.status(400).entity(e).build();
        }

        if (store.getSensors().containsKey(sensor.getId())) {
            Map<String, String> e = new HashMap<>();
            e.put("error", "Sensor already exists with id " + sensor.getId());
            return Response.status(409).entity(e).build();
        }

        if (sensor.getStatus() == null || sensor.getStatus().isEmpty()) {
            sensor.setStatus("ACTIVE");
        }

        store.getSensors().put(sensor.getId(), sensor);

        // also add to the rooms list of sensors
        Room room = store.getRooms().get(sensor.getRoomId());
        if (room != null) {
            room.getSensorIds().add(sensor.getId());
        }

        return Response.created(URI.create("/api/v1/sensors/" + sensor.getId()))
                .entity(sensor)
                .build();
    }

    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {

        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "No sensor found with id: " + sensorId);
            return Response.status(404).entity(err).build();
        }

        return Response.ok(sensor).build();
    }

    @DELETE
    @Path("/{sensorId}")
    public Response removeSensor(@PathParam("sensorId") String sensorId) {

        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "No sensor found with id: " + sensorId);
            return Response.status(404).entity(err).build();
        }

        // remove from parent room too
        Room parentRoom = store.getRooms().get(sensor.getRoomId());
        if (parentRoom != null) {
            parentRoom.getSensorIds().remove(sensorId);
        }

        store.getSensors().remove(sensorId);
        return Response.noContent().build();
    }

    // sub resource locator - passes control to SensorReadingResource for /readings
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadings(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

}