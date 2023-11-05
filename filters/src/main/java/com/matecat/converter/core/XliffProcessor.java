package com.matecat.converter.core;

import com.matecat.converter.core.okapiclient.OkapiClient;
import com.matecat.converter.core.okapiclient.OkapiPack;
import com.matecat.converter.core.util.Config;
import com.matecat.converter.core.winconverter.WinConverterRouter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Xliff Processor
 *
 * Processor of Xliff files, allowing:
 *  1. Extraction of the embedded pack (original file and manifest)
 *  2. Extraction of source and target languages
 */
public class XliffProcessor {

    // Logger
    private static Logger LOGGER = LoggerFactory.getLogger(XliffProcessor.class);

    private static final String CONVERTER_VERSION = XliffBuilder.class.getPackage().getImplementationVersion();
    private static final Pattern PRODUCER_CONVERTER_VERSION_PATTERN = Pattern.compile("matecat-converter(\\s+([^\"]+))?");

    // File we are processing
    private File xlf;

    // Embedded pack
    private OkapiPack pack;

    // Inner properties
    private String originalFilename = null;
    private Format originalFormat;
    private Locale sourceLanguage, targetLanguage;

    private boolean filterExtracted = false;
    private String filter;


    /**
     * Construct the processor given the XLF
     * @param xlf Xliff file we are going to process
     */
    public XliffProcessor(final File xlf) {

        // Check that the input file is not null
        if (xlf == null  ||  !xlf.exists()  ||  xlf.isDirectory())
            throw new IllegalArgumentException("The input file does not exist");

        // Check that the file is an .xlf
        if (!FilenameUtils.getExtension(xlf.getName()).equals("xlf"))
            throw new IllegalArgumentException("The input file is not a .xlf file");

        // Save the xlf
        this.xlf = xlf;

    }


    /**
     * Get source language
     * @return Source language
     */
    public Locale getSourceLanguage() {
        if (sourceLanguage == null)
            extractLanguages();
        return sourceLanguage;
    }


    /**
     * Get target language
     * @return Target language
     */
    public Locale getTargetLanguage() {
        if (targetLanguage == null)
            extractLanguages();
        return targetLanguage;
    }


    public String getFilter() {
        if (filterExtracted) return filter;

        filter = extractFilter();
        filterExtracted = true;
        return filter;
    }

    private String extractFilter() {
        InputStream inputStream = null;
        XMLStreamReader sax = null;
        try {
            inputStream = new FileInputStream(xlf);
            sax = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);

            // Default value is null
            String filter = null;

            // Look for a <file> tag in the first 1000 chars
            while (sax.hasNext() && sax.getLocation().getCharacterOffset() < 1000) {
                int event = sax.next();

                if (event == XMLStreamConstants.START_ELEMENT && sax.getLocalName().equals("file")) {
                    filter = sax.getAttributeValue(null, "filter");
                    break;
                }
            }

            return filter;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                if (sax != null) sax.close();
            } catch (XMLStreamException ignored) {}
            IOUtils.closeQuietly(inputStream);
        }
    }


    /**
     * Extract language from the XLF
     */
    private void extractLanguages() {
        try (InputStream inputStream = new FileInputStream(xlf)) {
            // Parse the XML document
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(inputStream);
            Element firstFile = (Element) document.getElementsByTagName("file").item(0);

            // Extract the languages
            this.sourceLanguage = new Locale(firstFile.getAttribute("source-language"));
            this.targetLanguage = new Locale(firstFile.getAttribute("target-language"));
        }
        catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException("Exception extracting source/target languages from MateCat xliff", e);
        }
    }


    /**
     * Get the original file embedded into the XLF
     * @return Original file
     */
    public File getOriginalFile() throws Exception {

        // Reconstruct the pack
        if (pack == null)
            reconstructPack();

        // Get the original file
        File originalFile = pack.getOriginalFile();

        // If it does not have its original format, try to convert it
        originalFile = convertToOriginalFormat(originalFile, originalFormat);

        // Return it
        return originalFile;

    }


    /**
     * Get the derived file
     * This is produced using the original file, the manifest and the XLF
     * @return Derived file
     */
    public File getDerivedFile() {

        // Reconstruct the pack
        if (pack == null)
            reconstructPack();

        // Generate the derived file
        File derivedFile = OkapiClient.generateDerivedFile(pack);

        // If it does not have its original format, try to convert it
        derivedFile = convertToOriginalFormat(derivedFile, originalFormat);

        // Return it
        return derivedFile;

    }


    /**
     * Try to convert a file to its original format
     * @param file File
     * @param originalFormat Original format
     * @return Converted file if possible, input file otherwise
     */
    private static File convertToOriginalFormat(File file, Format originalFormat) {
        Format currentFormat = Format.getFormat(file);
        if (Config.winConvEnabled && currentFormat != originalFormat && !Format.isOCRFormat(originalFormat)) {
            try {
                file = WinConverterRouter.convert(file, originalFormat);
            } catch (Exception e) {
                throw new RuntimeException("Exception while using WinConverterRouter.convert", e);
            }
        }
        return file;
    }


    /**
     * Reconstruct the original Okapi result pack from the embedded files
     */
    private void reconstructPack() {

        try (InputStream inputStream = new FileInputStream(xlf)) {

            // Output folder
            File packFolder = new File(xlf.getParentFile().getPath() + File.separator + "pack");
            if (packFolder.exists())
                FileUtils.cleanDirectory(packFolder);
            else
                packFolder.mkdir();

            // Parse the XML document
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(inputStream);

            NodeList fileElements = document.getElementsByTagName("file");
            Element originalFileElement = (Element) fileElements.item(0);
            Element manifestElement = (Element) fileElements.item(1);

            // Reconstruct the manifest
            String originalFilename = reconstructManifest(packFolder, manifestElement);

            checkProducerVersion(originalFileElement);

            reconstructOriginalFile(packFolder, originalFileElement, originalFilename);

            extractOriginalFormat(originalFileElement);

            // Extract the languages
            this.sourceLanguage = new Locale(originalFileElement.getAttribute("source-language"));
            this.targetLanguage = new Locale(originalFileElement.getAttribute("target-language"));

            // Reconstruct the original xlf
            reconstructOriginalXlf(packFolder, document, originalFileElement, manifestElement, originalFilename);

            // Generate the pack (which will check the extracted files)
            this.pack = new OkapiPack(packFolder);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException("Exception extracting Okapi pack from MateCat xliff", e);
        }

    }

    /**
     * Checks the tool-id attribute of the provided <file> element, extracts
     * the XLIFF producer converter version and logs some warnings if the
     * producer version does not match the version of this server.
     */
    private static void checkProducerVersion(Element file) {
        final String toolId = file.getAttribute("tool-id");
        if (toolId != null) {
            final Matcher matcher = PRODUCER_CONVERTER_VERSION_PATTERN.matcher(toolId);
            if (matcher.find()) {
                final String xliffVersion = matcher.group(2);
                if (xliffVersion == null) {
                    LOGGER.warn("Missing producer version in input XLIFF");
                } else {
                    if (CONVERTER_VERSION == null) {
                        LOGGER.warn("XLIFF producer version is " + xliffVersion + ", but server version is unknown (version available only when running from a jar)");
                    } else if (!xliffVersion.equals(CONVERTER_VERSION)) {
                        LOGGER.warn("Converters versions mismatch: " + xliffVersion + " (XLIFF) vs " + CONVERTER_VERSION + " (server)");
                    } else {
                        // In this last condition converters versions match,
                        // so everything is perfect!
                    }
                }
            } else {
                LOGGER.warn("Bad tool-id attribute");
            }
        }
    }


    /**
     * Extract the original format from the embedded information
     * @param fileElement XML element from the file
     */
    private void extractOriginalFormat(Element fileElement) {
        try {
            String filename = fileElement.getAttribute("original");
            this.originalFormat = Format.getFormat(filename);
        }
        catch (Exception e1) {
            throw new RuntimeException("The encoded file has no extension");
        }
    }


    /**
     * Get the original filename from the embedded information
     * @param fileElement XML element from the file
     * @return Filename
     */
    private String getFilename(Element fileElement) {

        // Filename
        String filename = fileElement.getAttribute("original");

        // Replace the extension of the file for the one it was converted to
        // Datatype structure is:  datatype="x-{FORMAT (after conversions)}"
        try {
            String datatype = fileElement.getAttribute("datatype");
            String convertedExtension = datatype.substring(2);
            filename = FilenameUtils.getBaseName(filename) + "." + convertedExtension;
        }
        catch (Exception ignore) {}

        // Return it
        return filename;

    }


    /**
     * Extract the original file and save it in the pack
     * @param packFolder Pack's folder
     * @param fileElement XML element containing the file
     */
    private void reconstructOriginalFile(File packFolder, Element fileElement, String originalFilename) {

        try {

            // Filename
            if (originalFilename == null) {
                originalFilename = getFilename(fileElement);
            }

            // Contents
            Element internalFileElement = (Element) fileElement.getElementsByTagName("internal-file").item(0);
            String encodedFile = internalFileElement.getTextContent().trim();
            byte[] originalFileBytes = Base64.getDecoder().decode(encodedFile);

            // Create original folder
            File originalFolder = new File(packFolder.getPath() + File.separator + OkapiPack.ORIGINAL_DIRECTORY_NAME);
            if (originalFolder.exists())
                FileUtils.cleanDirectory(originalFolder);
            else
                originalFolder.mkdir();

            // Reconstruct the original file
            File originalFile = new File(originalFolder.getPath() + File.separator + originalFilename);
            originalFile.createNewFile();
            FileUtils.writeByteArrayToFile(originalFile, originalFileBytes);

        }
        catch (Exception e) {
            throw new RuntimeException("Exception extracting original file from MateCat xliff", e);
        }
    }


    /**
     * Reconstruct the manifest and save it in the pack
     * @param packFolder Pack's folder
     * @param manifestElement XML element containing the manifest
     */
    private String reconstructManifest(File packFolder, Element manifestElement) {

        try {

            // Check that it's the manifest
            if (!manifestElement.getAttribute("original").equals(OkapiPack.MANIFEST_FILENAME))
                throw new RuntimeException("The xlf is corrupted: it does not contain a manifest");

            // Extract language
            String targetLanguage = manifestElement.getAttribute("target-language");

            // Manifest contents
            Element internalFileElement = (Element) manifestElement.getElementsByTagName("internal-file").item(0);
            String encodedManifest = internalFileElement.getTextContent().trim();
            String manifest = new String(Base64.getDecoder().decode(encodedManifest), StandardCharsets.UTF_8);
            // MateCAT caches produced XLIFFs and reuses them to save
            // file conversions, updating just the source and target
            // languages when needed.
            // But this creates a problem: the Okapi's Manifest file
            // maintains the original couple of source - target
            // languages.
            // So sometimes this happens: Okapi runs looking for
            // segments in the language specified in the Manifest,
            // but in the XLIFF the segments are all in another
            // language, so Okapi finds nothing.
            // Missing target segments means obtaining a file
            // identical to the original, without translations.
            // To fix this I replace the target in the manifest with
            // the one defined in the XLIFF.
            // TODO: NX: cause of no target?
            manifest = manifest.replaceFirst("(<manifest [^>]* ?target=\")[^\"]+\"", "$1" + targetLanguage + "\"");

            // Extract source filename from manifest
            // Originally this class used to extract the original filename
            // from the "original" attribute of the first <file> element in
            // the XLIFF. Unfortunately some bugs in the encoding of the
            // filename in the HTTP communication caused many XLIFFs to be
            // created with corrupted text inside the "original" attribute.
            // So the pack was reconstructed using the "original" attribute,
            // but Okapi could not find the files because the filenames
            // in the manifest were different. To solve this bug I ignore
            // the "original" attribute and extract it directly from manifest.
            // TODO: remove the "original" attribute and rethink class design
            Matcher matcher = Pattern.compile(" relativeInputPath *= *\"(.+?)\"").matcher(manifest);
            String originalFilename = (matcher.find() ? StringEscapeUtils.unescapeXml(matcher.group(1)) : null);

            // Reconstruct the manifest file
            File manifestFile = new File(packFolder.getPath() + File.separator + OkapiPack.MANIFEST_FILENAME);
            manifestFile.createNewFile();
            FileUtils.writeStringToFile(manifestFile, manifest, StandardCharsets.UTF_8);

            return originalFilename;
        }
        catch (Exception e) {
            throw new RuntimeException("Exception extracting Okapi manifest from MateCat xliff", e);
        }
    }


    /**
     * Reconstruct the original XLF used to derive this XLF; and save it into the work folder
     * inside the pack
     *
     * This is done by simply removing the file and manifest XML elements.
     * @param packFolder Pack's folder
     * @param document XML document
     * @param fileElement XML element containing the file
     * @param manifestElement XML element containing the manifest
     */
    private void reconstructOriginalXlf(File packFolder, Document document, Element fileElement, Element manifestElement, String originalFilename) {

        try {

            // Get root
            Element root = document.getDocumentElement();

            // Filename
            if (originalFilename == null) {
                originalFilename = getFilename(fileElement);
            }

            // Obtain the original xlf
            root.removeChild(fileElement);
            root.removeChild(manifestElement);

            // Remove the leading underscore added to all the <ex> ids by the
            // XliffBuilder (see the comment there for more background)
            NodeList exElements = document.getElementsByTagName("ex");
            for (int i = 0; i < exElements.getLength(); i++) {
                Element exElement = (Element) exElements.item(i);
                if (exElement.getAttribute("id").startsWith("_")) {
                    exElement.setAttribute("id", exElement.getAttribute("id").substring(1));
                }
            }

            // Create work folder
            File workFolder = new File(packFolder.getPath() + File.separator + OkapiPack.WORK_DIRECTORY_NAME);
            if (workFolder.exists())
                FileUtils.cleanDirectory(workFolder);
            else
                workFolder.mkdir();

            // Save the file

            String xlfOutputPath = workFolder.getPath() + File.separator + originalFilename + ".xlf";

            // The Java Transformer doesn't update the XML prolog with the
            // output encoding.
            // For example, if you read a UTF-16 XML and rewrite it as UTF-8,
            // the Transformer still writes the prolog with "encoding=UTF-16",
            // messing up the file.
            // Since our output encoding will always be UTF-8 (because we use
            // FileOutputStream, that uses the default Java charset, that we
            // ensured is UTF-8 in the Main class) I tell Transformer to not
            // write the prolog and I write it myself in the correct way.
            try (OutputStream outputStream = new FileOutputStream(xlfOutputPath)) {
                outputStream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes());
                StreamResult streamResult = new StreamResult(outputStream);

                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                DOMSource domSource = new DOMSource(document);
                transformer.transform(domSource, streamResult);
            }

        } catch (TransformerException | IOException e) {
            e.printStackTrace();
        }
    }

}