package com.matecat.converter.core.project;

import com.matecat.converter.core.util.Config;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project class
 *
 * It represents a project created from one file. A project has it's own folder,
 * and is guaranteed to not have name collisions with other projects.
 *
 * Projects can be created by means of the ProjectFactory class.
 *
 * @see ProjectFactory
 */
public class Project {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(Project.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH-mm");

    // Inner properties
    private File folder;
    private File file;

    /**
     * Contructor invoked by the factory, receiving a file
     *
     * @param file Project's file
     */
    protected Project(File file) {
        this.file = file;
        this.folder = file.getParentFile();
    }

    /**
     * Get folder
     *
     * @return Project's folder
     */
    public File getFolder() {
        if (folder == null) {
            throw new RuntimeException("The project has already been deleted");
        }
        return folder;
    }

    /**
     * Get file
     *
     * @return Project's file
     */
    public File getFile() {
        if (file == null) {
            throw new RuntimeException("The project has already been deleted");
        }
        return file;
    }

    /**
     * Close the project
     *
     * This will remove all the inner references to the file, and remove the
     * folder depending on the configuration One this method is executed, it's
     * not possible to use the project again.
     *
     * @param success
     */
    public void close(boolean success) {
        if (!success && !Config.ERRORS_FOLTER.isEmpty()) {
            final Date now = Calendar.getInstance().getTime();
            final String errorFolderPath = Config.ERRORS_FOLTER
                    + File.separator + DATE_FORMAT.format(now)
                    + File.separator + TIME_FORMAT.format(now)
                    + "-" + folder.getName();

            final File errorFolder = new File(errorFolderPath);
            errorFolder.getParentFile().mkdirs();

            boolean moveError = false;
            if (Config.DELETE_ON_CLOSE) {
                try {
                    FileUtils.moveDirectory(folder, errorFolder);
                    LOGGER.info("Folder with temp files moved to " + errorFolderPath);
                } catch (Exception e) {
                    moveError = true;
                    LOGGER.error("Exception moving temp folder to errors folder; will try to copy it", e);
                }
            }

            if (!Config.DELETE_ON_CLOSE || moveError) {
                try {
                    FileUtils.copyDirectory(folder, errorFolder);
                } catch (Exception e) {
                    LOGGER.error("Exception while copying temp folder to errors folder", e);
                }
            }
        }
        if (Config.DELETE_ON_CLOSE && folder.exists()) {
            try {
                FileUtils.deleteDirectory(folder);
            } catch (IOException e) {
                LOGGER.error("Exception while deleting temp folder", e);
            }
        }
        this.folder = null;
        this.file = null;
    }

}
