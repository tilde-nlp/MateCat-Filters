package com.matecat.converter.server.resources;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;

/**
 * Client resource
 */
@Path("/")
public class ClientResource {

    private static final String CLIENT_HTML_PATH = "client/client.html";
    private static final String CLIENT_HTML;

    static {
        String filtersVersion = ClientResource.class.getPackage().getImplementationVersion();
        if (filtersVersion == null) {
            filtersVersion = "[version number not available]";
        }
        try {
            String html = IOUtils.toString(ClientResource.class.getClassLoader().getResourceAsStream(CLIENT_HTML_PATH), "UTF-8");
            // Handlebar-like variable substitution
            CLIENT_HTML = html.replace("{{filtersVersion}}", filtersVersion);
        } catch (IOException e) {
            throw new RuntimeException("The client.html was not found");
        }
    }

    /**
     * Output the client
     *
     * @param request
     * @return Client
     * @throws java.io.IOException
     */
    @GET
    public Response client(@Context HttpServletRequest request) throws IOException {
        return Response
                .status(Response.Status.OK)
                .entity(CLIENT_HTML)
                .build();
    }

}
