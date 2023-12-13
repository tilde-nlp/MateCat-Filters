package com.matecat.converter.core.encoding;

import java.io.File;
import java.net.URISyntaxException;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class EncodingDetectorRouterTest {

    EncodingDetectorRouter detector;

    @Before
    public void setUp() throws Exception {
        detector = new EncodingDetectorRouter();
    }

    private File getTestFile(String name) throws URISyntaxException {
        return new File(getClass().getResource("/encoding/" + name).toURI());
    }

    @Test
    public void testDetectHTML5UTF8() throws Exception {
        File testFile = getTestFile("UTF-8.html");
        Encoding match = detector.detect(testFile);
        assertEquals("UTF-8", match.getCode());
    }

    @Test
    public void testDetectHTML5FakeUTF16() throws Exception {
        File testFile = getTestFile("UTF-16-fake.html");
        Encoding match = detector.detect(testFile);
        assertEquals("UTF-16", match.getCode());
    }

    @Test
    public void testDetectHTML5UTF16LE() throws Exception {
        File testFile = getTestFile("UTF-16LE.html");
        Encoding match = detector.detect(testFile);
        assertEquals("UTF-16LE", match.getCode());
    }

    @Test
    public void testDetectHTM() throws Exception {
        File testFile = getTestFile("ISO-8859-1.html");
        Encoding match = detector.detect(testFile);
        assertEquals("ISO-8859-1", match.getCode());
    }

    @Test
    public void testDetectTXTUTF8() throws Exception {
        File testFile = getTestFile("UTF-8.txt");
        Encoding match = detector.detect(testFile);
        assertEquals("UTF-8", match.getCode());
    }

    @Test
    public void testDetectTXTUTF16LE() throws Exception {
        File testFile = getTestFile("UTF-16LE.txt");
        Encoding match = detector.detect(testFile);
        assertEquals("UTF-16LE", match.getCode());
    }

    @Test
    public void testDetectTXTWindows1252() throws Exception {
        File testFile = getTestFile("windows-1252.txt");
        Encoding match = detector.detect(testFile);
        assertEquals("ISO-8859-1", match.getCode());
    }

    @Test
    public void testDetectBinary() throws Exception {
        File testFile = getTestFile("test.docx");
        Encoding match = detector.detect(testFile);
        assertEquals(Encoding.DEFAULT, match.getCode());
    }

}
