/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.smartcampus.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

// logs every request and response so we can see whats happening
@Provider
public class ApiLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger logger = Logger.getLogger(ApiLoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        String requestId = java.util.UUID.randomUUID().toString().substring(0, 8);
        req.setProperty("reqId", requestId);
        req.setProperty("startTime", System.currentTimeMillis());

        logger.info("[" + requestId + "] " + req.getMethod() + " " + req.getUriInfo().getRequestUri());
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        String requestId = (String) req.getProperty("reqId");
        Long start = (Long) req.getProperty("startTime");
        long duration = start != null ? System.currentTimeMillis() - start : 0;

        logger.info("[" + requestId + "] " + req.getMethod().toString() + " " + req.getUriInfo().getRequestUri().toString());
    }

}