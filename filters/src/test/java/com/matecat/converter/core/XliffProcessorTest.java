package com.matecat.converter.core;

import com.matecat.converter.core.okapiclient.OkapiPack;
import com.matecat.filters.basefilters.DefaultFilter;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Locale;
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * Xliff processor Test
 */
public class XliffProcessorTest {

    /**
     * Test the extraction of the original file without problems
     *
     * @throws Exception
     */
    @Test
    public void testGetOriginalFile() throws Exception {
        File xlf = new File(getClass().getResource("/extraction/test.docx.xlf").getPath());
        new XliffProcessor(xlf).getOriginalFile();
    }

    /**
     * Test that the file is derived and, in special, that it is translated
     *
     * @throws Exception
     */
    @Test
    public void testGetDerivedFile() throws Exception {

        // Constants to work with
        final String ORIGINAL = "TEXT _BEFORE_ TRANSLATION";
        final String DERIVED = "TEXT _AFTER_ TRANSLATION";

        // Create text file
        File folder = new File(getClass().getResource("/derivation").getPath());
        File input = new File(folder.getPath() + File.separator + "test.txt");
        FileUtils.writeStringToFile(input, ORIGINAL, Charset.forName("UTF-8"));

        // Generate xlf
        DefaultFilter generator = new DefaultFilter();
        File xlf = generator.extract(input, Locale.ENGLISH, Locale.ENGLISH, null);

        // Remove pack
        File pack = new File(folder.getPath() + File.separator + OkapiPack.PACK_FILENAME);
        FileUtils.deleteDirectory(pack);

        // Alter the translation
        String xlfContent = FileUtils.readFileToString(xlf, "UTF-8");
        String newXlfContent = xlfContent.replaceAll(ORIGINAL + "</mrk></target>", DERIVED + "</mrk></target>");
        FileUtils.writeStringToFile(xlf, newXlfContent, Charset.forName("UTF-8"));

        // Generate derived and check
        File output = new XliffProcessor(xlf).getDerivedFile();
        String content = FileUtils.readFileToString(output, "UTF-8");
        assertEquals(DERIVED, content);

    }

}
