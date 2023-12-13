package com.matecat.converter.core.project;

import com.matecat.converter.core.util.Config;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory in charge of creating projects, in temporal folders
 */
public class ProjectFactory {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectFactory.class);


    /**
     * Create a project given the filename and content of a file
     *
     * @param filename Filename
     * @param uploadedInputStream Input stream containing the contents of the
     * file. It will be closed after processing.
     * @return Project containing the file
     */
    public static Project createProject(String filename, InputStream uploadedInputStream) {
        try {

            // Load the folder
            File folder = Files.createTempDirectory(Paths.get(Config.CACHE_FOLDER), "").toFile();

            // Save the file inside the temporal
            File file = new File(folder.getPath() + File.separator + filename);
            FileUtils.copyInputStreamToFile(uploadedInputStream, file);

            // Return the project
            Project project = new Project(file);
            LOGGER.info("Caching files in {}", project.getFolder().getPath());
            return project;
        } catch (IOException e) {
            LOGGER.error("Exception creating cache folder", e);
            throw new ProjectCreationException(String.format("It was not possible to create a project for the file '%s'", filename));
        }
    }
    /**
     * Private constructor to make the class static
     */
    private ProjectFactory() {
    }

}
