package com.matecat.converter.server.resources;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple resource used to test if the server is running
 */
@Path("/test")
public class TestConnectionResource {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(TestConnectionResource.class);

    /**
     * @param request
     * @return a simple ok message
     */
    @GET
    public Response test(@Context HttpServletRequest request) {
        LOGGER.info("TEST request from {}", request.getRemoteAddr());
        return Response
                .status(Response.Status.OK)
                .entity("Server on")
                .build();
    }

}
