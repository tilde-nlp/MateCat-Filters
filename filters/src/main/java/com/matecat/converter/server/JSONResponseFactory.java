package com.matecat.converter.server;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import org.json.simple.JSONObject;

/**
 * Factory which creates JSON messages to use as http responses
 *
 * TODO: Replace the JSON response by the contents of the file. This class will
 * be deleted.
 */
public class JSONResponseFactory {

    public static final String IS_SUCCESS = "isSuccess";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String XLIFF_CONTENT = "xliffContent";
    public static final String DOCUMENT_CONTENT = "documentContent";
    public static final String FILENAME = "filename";

    public static String getError(String errorMessage) {
        JSONObject output = new JSONObject();
        output.put(IS_SUCCESS, false);
        output.put(ERROR_MESSAGE, errorMessage);
        return output.toJSONString();
    }

    public static String getConvertSuccess(File file) {
        try {
            String xliffContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            JSONObject output = new JSONObject();
            output.put(IS_SUCCESS, true);
            output.put(XLIFF_CONTENT, xliffContent);
            output.put(FILENAME, file.getName());
            return output.toJSONString();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public static String getDerivedSuccess(File file) {
        try {
            String encodedDocument = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
            JSONObject output = new JSONObject();
            output.put(IS_SUCCESS, true);
            output.put(DOCUMENT_CONTENT, encodedDocument);
            output.put(FILENAME, file.getName());
            return output.toJSONString();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

}
