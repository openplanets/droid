/**
 * <p>Copyright (c) The National Archives 2005-2010.  All rights reserved.
 * See Licence.txt for full licence details.
 * <p/>
 *
 * <p>DROID DCS Profile Tool
 * <p/>
 */
package uk.gov.nationalarchives.droid.container.ole2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import uk.gov.nationalarchives.droid.container.ContainerFile;
import uk.gov.nationalarchives.droid.container.ContainerSignature;
import uk.gov.nationalarchives.droid.container.ContainerSignatureSaxParser;
import uk.gov.nationalarchives.droid.container.FileFormatMapping;
import uk.gov.nationalarchives.droid.core.interfaces.DroidCore;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationRequest;
import uk.gov.nationalarchives.droid.core.interfaces.IdentificationResultCollection;
import uk.gov.nationalarchives.droid.core.interfaces.RequestIdentifier;
import uk.gov.nationalarchives.droid.core.interfaces.archive.ArchiveFormatResolver;
import uk.gov.nationalarchives.droid.core.interfaces.archive.ContainerIdentifierFactory;
import uk.gov.nationalarchives.droid.core.interfaces.archive.IdentificationRequestFactory;
import uk.gov.nationalarchives.droid.core.interfaces.resource.RequestMetaData;

/**
 * @author rflitcroft
 *
 */
@Ignore
public class Ole2RootFileTest {

    private Ole2Identifier ole2Identifier;
    
    
    @Before
    public void setup() {
        ole2Identifier = new Ole2Identifier();
    }
    
    
    @Test
    public void testIdentifyWordDocumentFromRootFile() throws IOException {
        
        ContainerSignature sig = new ContainerSignature();
        sig.setId(100);
        sig.setDescription("Word 97 OLE2");
        
        ContainerFile containerFile = new ContainerFile();
        containerFile.setPath("WordDocument");
        sig.setFiles(Arrays.asList(new ContainerFile[] {containerFile}));
        
        Map<Integer, List<FileFormatMapping>> formats = new HashMap<Integer, List<FileFormatMapping>>();
        FileFormatMapping fileFormat = new FileFormatMapping();
        fileFormat.setPuid("fmt/666");
        List<FileFormatMapping> formatMapping = new ArrayList<FileFormatMapping>();
        formatMapping.add(fileFormat);
        formats.put(100, formatMapping);
        
        
        ole2Identifier.addContainerSignature(sig);
        ole2Identifier.setFormats(formats);
        
        InputStream word97Stream = getClass().getClassLoader().getResourceAsStream("word97.doc");
        
        RequestMetaData metaData = mock(RequestMetaData.class);
        IdentificationRequest request = mock(IdentificationRequest.class);
        when(request.getSourceInputStream()).thenReturn(word97Stream);
        when(request.getRequestMetaData()).thenReturn(metaData);
        RequestIdentifier requestIdentifier = mock(RequestIdentifier.class);
        when(request.getIdentifier()).thenReturn(requestIdentifier);
        
        IdentificationResultCollection results = ole2Identifier.submit(request);
        
        assertEquals("fmt/666", results.getResults().iterator().next().getPuid());
    }

    @Test
    public void testIdentifyWordDocumentFromRootFileAndCompObj() throws IOException {
        
        ContainerSignature sig = new ContainerSignature();
        sig.setId(100);
        sig.setDescription("Word 97 OLE2");
        
        ContainerFile rootFile = new ContainerFile();
        rootFile.setPath("WordDocument");

        ContainerFile compObj = new ContainerFile();
        compObj.setPath("CompObj");
        //compObj.setTextSignature(".*Word.Document.8.*");

        sig.setFiles(Arrays.asList(new ContainerFile[] {rootFile, compObj}));
        
        Map<Integer, List<FileFormatMapping>> formats = new HashMap<Integer, List<FileFormatMapping>>();
        FileFormatMapping fileFormat = new FileFormatMapping();
        fileFormat.setPuid("fmt/666");
        List<FileFormatMapping> formatMapping = new ArrayList<FileFormatMapping>();
        formatMapping.add(fileFormat);
        formats.put(100, formatMapping);
        
        
        ole2Identifier.addContainerSignature(sig);
        ole2Identifier.setFormats(formats);
        
        InputStream word97Stream = getClass().getClassLoader().getResourceAsStream("word97.doc");
        
        RequestMetaData metaData = mock(RequestMetaData.class);
        IdentificationRequest request = mock(IdentificationRequest.class);
        when(request.getSourceInputStream()).thenReturn(word97Stream);
        when(request.getRequestMetaData()).thenReturn(metaData);
        RequestIdentifier requestIdentifier = mock(RequestIdentifier.class);
        when(request.getIdentifier()).thenReturn(requestIdentifier);
        
        IdentificationRequestFactory requestFactory = mock(IdentificationRequestFactory.class);
        when(requestFactory.newRequest(null, null))
            .thenReturn(request);
        ole2Identifier.setRequestFactory(requestFactory);
        
        IdentificationResultCollection results = ole2Identifier.submit(request);
        
        assertEquals("fmt/666", results.getResults().iterator().next().getPuid());
    }

    @Test
    public void testIdentifyContainerUsingCompObj() throws IOException {
        
        ContainerSignature wordSig = new ContainerSignature();
        wordSig.setId(100);
        wordSig.setDescription("Word 97 OLE2");
        
        ContainerFile wordDocument = new ContainerFile();
        wordDocument.setPath("WordDocument");

        ContainerFile wordCompObj = new ContainerFile();
        wordCompObj.setPath("CompObj");
        //wordCompObj.setTextSignature(".*Word.Document.8.*");

        wordSig.setFiles(Arrays.asList(new ContainerFile[] {wordDocument, wordCompObj}));
        
        ContainerSignature excelSig = new ContainerSignature();
        excelSig.setId(100);
        excelSig.setDescription("Word 97 OLE2");
        
        ContainerFile workbook = new ContainerFile();
        workbook.setPath("Workbook");

        ContainerFile excelCompObj = new ContainerFile();
        excelCompObj.setPath("CompObj");
        //excelCompObj.setTextSignature(".*Word.Document.8.*");

        excelSig.setFiles(Arrays.asList(new ContainerFile[] {workbook, excelCompObj}));

        Map<Integer, List<FileFormatMapping>> formats = new HashMap<Integer, List<FileFormatMapping>>();
        FileFormatMapping fileFormat = new FileFormatMapping();
        fileFormat.setPuid("fmt/666");
        List<FileFormatMapping> formatMapping = new ArrayList<FileFormatMapping>();
        formatMapping.add(fileFormat);
        formats.put(100, formatMapping);
        
        
        ole2Identifier.addContainerSignature(wordSig);
        ole2Identifier.setFormats(formats);
        
        InputStream word97Stream = getClass().getClassLoader().getResourceAsStream("word97.doc");
        
        RequestMetaData metaData = mock(RequestMetaData.class);
        IdentificationRequest request = mock(IdentificationRequest.class);
        when(request.getSourceInputStream()).thenReturn(word97Stream);
        when(request.getRequestMetaData()).thenReturn(metaData);
        RequestIdentifier requestIdentifier = mock(RequestIdentifier.class);
        when(request.getIdentifier()).thenReturn(requestIdentifier);
        
        IdentificationRequestFactory requestFactory = mock(IdentificationRequestFactory.class);
        when(requestFactory.newRequest(null, null)).thenReturn(request);
        ole2Identifier.setRequestFactory(requestFactory);
        
        IdentificationResultCollection results = ole2Identifier.submit(request);
        
        assertEquals("fmt/666", results.getResults().iterator().next().getPuid());
    }
    
    @Test
    public void testInitialiseRegistersOle2ContainerIdentifierWithContainerIdentifierResolver() throws Exception {
        
        URL containerSignatureUrl = getClass().getClassLoader().getResource("container-signature.xml");
        String path = containerSignatureUrl.getPath();
        
        ContainerIdentifierFactory containerIdentifierFactory = mock(ContainerIdentifierFactory.class);
        ArchiveFormatResolver containerFormatResolver = mock(ArchiveFormatResolver.class);
        
        DroidCore droidCore = mock(DroidCore.class);
        ole2Identifier.setDroidCore(droidCore);

        ole2Identifier.setContainerIdentifierFactory(containerIdentifierFactory);
        ole2Identifier.setContainerFormatResolver(containerFormatResolver);
        
        ole2Identifier.setSignatureFileParser(new ContainerSignatureSaxParser());
        ole2Identifier.setContainerType("OLE2");
        ole2Identifier.setSignatureFilePath(path);
        IdentificationRequestFactory requestFactory = mock(IdentificationRequestFactory.class);
        
        IdentificationRequest request = mock(IdentificationRequest.class);
        when(requestFactory.newRequest(null, null))
            .thenReturn(request);
        ole2Identifier.setRequestFactory(requestFactory);

        ole2Identifier.init();
        
        verify(containerIdentifierFactory).addContainerIdentifier("OLE2", ole2Identifier);
        
    }

    @Test
    public void testInitialiseRegistersOle2ContainerFormatsAgainstOoxmlPuid() throws Exception {
        
        URL containerSignatureUrl = getClass().getClassLoader().getResource("container-signature.xml");
        String path = containerSignatureUrl.getPath();
        
        ContainerIdentifierFactory containerIdentifierFactory = mock(ContainerIdentifierFactory.class);
        ArchiveFormatResolver containerFormatResolver = mock(ArchiveFormatResolver.class);
        
        DroidCore droidCore = mock(DroidCore.class);
        ole2Identifier.setDroidCore(droidCore);

        ole2Identifier.setContainerIdentifierFactory(containerIdentifierFactory);
        ole2Identifier.setContainerFormatResolver(containerFormatResolver);
        
        ole2Identifier.setSignatureFileParser(new ContainerSignatureSaxParser());
        ole2Identifier.setContainerType("OLE2");
        ole2Identifier.setSignatureFilePath(path);
        ole2Identifier.init();
        
        verify(containerIdentifierFactory).addContainerIdentifier("OLE2", ole2Identifier);
        verify(containerFormatResolver).registerPuid("fmt/111", "OLE2");
        
    }

    @Test
    public void testInitialiseDeregistersOle2BinarySignaturesFromDroid4() throws Exception {
        
        URL containerSignatureUrl = getClass().getClassLoader().getResource("container-signature.xml");
        String path = containerSignatureUrl.getPath();
        
        ContainerIdentifierFactory containerIdentifierFactory = mock(ContainerIdentifierFactory.class);
        ArchiveFormatResolver containerFormatResolver = mock(ArchiveFormatResolver.class);
        
        ole2Identifier.setContainerIdentifierFactory(containerIdentifierFactory);
        ole2Identifier.setContainerFormatResolver(containerFormatResolver);
        DroidCore droidCore = mock(DroidCore.class);
        ole2Identifier.setDroidCore(droidCore);
        
        ole2Identifier.setSignatureFileParser(new ContainerSignatureSaxParser());
        ole2Identifier.setSignatureFilePath(path);
        ole2Identifier.init();
        
        verify(droidCore).removeSignatureForPuid("fmt/41");
        verify(droidCore).removeSignatureForPuid("fmt/43");
        
    }
}
