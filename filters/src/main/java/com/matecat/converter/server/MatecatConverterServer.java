package com.matecat.converter.server;

import com.matecat.converter.core.util.Config;
import com.matecat.converter.server.resources.ConvertToXliffResource;
import com.matecat.converter.server.resources.ExtractOriginalFileResource;
import com.matecat.converter.server.resources.GenerateDerivedFileResource;
import com.matecat.converter.server.resources.TestConnectionResource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.InetAddress;
import java.net.URL;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Matecat converter server
 */
public class MatecatConverterServer {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(MatecatConverterServer.class);

    // Used port
    private int serverPort;

    // Server
    private Server server;
    private String localIP, externalIP;

    /**
     * Constructor which will use the default port
     */
    public MatecatConverterServer() {
        try {
            int port = Config.SERVER_PORT;
            if (port <= 0) {
                throw new Exception();
            }
            this.serverPort = port;
            init();
        } catch (Exception e) {
            throw new RuntimeException("There is no default port specified in the configuration");
        }
    }

    /**
     * Constructor admitting a configured port
     *
     * @param serverPort Port to use
     */
    public MatecatConverterServer(int serverPort) {
        if (serverPort < 0) {
            throw new IllegalArgumentException("There port specified in the configuration is not valid");
        }
        this.serverPort = serverPort;
        init();
    }

    /**
     * Stop the server
     */
    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            LOGGER.error("It was not possible to stop the server", e);
        }
    }

    /**
     * Check if the server has been started (this is, ready to receive requests)
     *
     * @return True if started, false otherwise
     */
    public boolean isStarted() {
        return server.isStarted();
    }

    /**
     * Check if the server is stopped
     *
     * @return True if stopped, false otherwise
     */
    public boolean isStopped() {
        return server.isStopped();
    }

    /**
     * Get external IP
     *
     * @return External IP
     */
    private String getExternalIP() {
        if (externalIP == null) {
            try {
                URL whatismyip = new URL("http://checkip.amazonaws.com");
                try ( BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()))) {
                    externalIP = in.readLine();
                }
            } catch (IOException ignored) {
            }
        }
        return externalIP;
    }

    /**
     * Get local IP
     *
     * @return Local IP
     */
    private String getLocalIP() {
        if (localIP == null) {
            try {
                localIP = InetAddress.getLocalHost().getHostAddress();
            } catch (IOException ignored) {
            }
        }
        return localIP;
    }

    /**
     * Init the server
     */
    private void init() {
        try {
            initServer();
            server.start();
            LOGGER.info("Server started at {}:{} / {}:{}", getExternalIP(), serverPort, getLocalIP(), serverPort);
        } catch (BindException e) {
            LOGGER.error("Port " + serverPort + " already in use");
            System.exit(-1);
        } catch (InterruptedException e) {
            LOGGER.error("Server has been interrupted", e);
            throw new RuntimeException("The server has been interrupted");
        } catch (Exception e) {
            LOGGER.error("Exception starting the server", e);
            throw new RuntimeException("Unknown internal server problem");
        }
    }

    /**
     * Initialize the resources and other aspects of the server
     */
    private void initServer() {

        // Configure the server
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages(ConvertToXliffResource.class.getPackage().getName());
        resourceConfig.packages(GenerateDerivedFileResource.class.getPackage().getName());
        resourceConfig.packages(ExtractOriginalFileResource.class.getPackage().getName());
        resourceConfig.packages(TestConnectionResource.class.getPackage().getName());
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(MultiPartFeature.class);
        ServletContainer servletContainer = new ServletContainer(resourceConfig);
        ServletHolder sh = new ServletHolder(servletContainer);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);


        String contextPath = System.getenv("MATECAT_API_CONTEXT_PATH");

        if (contextPath == null || contextPath == "") {
            contextPath = "/";
        }

        LOGGER.info("MateCat starting using contextPath {}", contextPath);

        context.setContextPath(contextPath);
        context.addServlet(sh, "/*");

        // Initiate it
        this.server = new Server(serverPort);
        server.setHandler(context);
        LOGGER.info("initServer DONE");
    }

}
