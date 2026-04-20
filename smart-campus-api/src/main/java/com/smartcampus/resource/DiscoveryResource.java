package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    // this endpoint just returns info about the api, like a welcome page
    @GET
    public Response getApiInfo() {

        Map<String, Object> info = new HashMap<>();
        info.put("name", "Smart Campus API");
        info.put("version", "1.0");
        info.put("description", "API for managing rooms and sensors on campus");

        Map<String, String> contact = new HashMap<>();
        contact.put("admin", "w2113683@westminster.ac.uk");
        info.put("contact", contact);

        // links to the main resources so clients know where to go
        Map<String, String> links = new HashMap<>();
        links.put("rooms", "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        info.put("links", links);

        return Response.ok(info).build();
    }

}