package com.matecat.converter.server.resources;

import com.matecat.converter.server.JSONResponseFactory;
import com.matecat.converter.server.MatecatConverterServer;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import javax.ws.rs.Path;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;


public class GenerateDerivedFileResourceTest {

    private static MatecatConverterServer server;
    private static final int PORT = 8090;
    private static final String url = "http://localhost:" + PORT + GenerateDerivedFileResource.class.getAnnotation(Path.class).value();

    @Before
    public void setUp() throws Exception {
        server = new MatecatConverterServer(PORT);
        while ( !server.isStarted() )
            Thread.sleep(100);
    }

    @Test
    public void testDeriveSuccess() throws Exception {

        File fileToUpload = new File(getClass().getResource("/server/test.docx.xlf").getPath());

        // Send request
        HttpClient httpclient = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(url);
        FileBody uploadFilePart = new FileBody(fileToUpload);
        MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
        reqEntity.addPart("xliffContent", uploadFilePart);
        httpPost.setEntity(reqEntity.build());
        HttpResponse response = httpclient.execute(httpPost);

        // Check OK status code
        assertEquals(200, response.getStatusLine().getStatusCode());

        // Check body
        String body = new BufferedReader(new InputStreamReader(response.getEntity().getContent())).readLine();
        JSONObject json = (JSONObject) new JSONParser().parse(body);

        // Is success
        boolean isSuccess = (boolean) json.get(JSONResponseFactory.IS_SUCCESS);
        assertTrue(isSuccess);

        // No error message
        String error = (String) json.getOrDefault(JSONResponseFactory.ERROR_MESSAGE, "");
        assertEquals("", error);

        // Encoded document
        String encodedDoc = (String) json.get(JSONResponseFactory.DOCUMENT_CONTENT);
        assertNotSame("", encodedDoc);

    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        while (!server.isStopped())
            Thread.sleep(100);
    }
}