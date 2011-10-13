/**
 * <p>Copyright (c) The National Archives 2005-2010.  All rights reserved.
 * See Licence.txt for full licence details.
 * <p/>
 *
 * <p>DROID DCS Profile Tool
 * <p/>
 */
package uk.gov.nationalarchives.droid.core.interfaces.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import uk.gov.nationalarchives.droid.core.interfaces.RequestIdentifier;
import uk.gov.nationalarchives.droid.core.interfaces.archive.ArchiveFileUtils;


public class TarEntryIdentificationRequestTest {

    private static File tmpDir;
    private TarEntryIdentificationRequest tarResource;
    private String fileName;
    private TarArchiveInputStream in;
    private String entryName;
    private long size;
    private Date modTime;
    private RequestMetaData metaData;
    private RequestIdentifier identifier;
    
    @BeforeClass
    public static void createTmpFileDirectory() {
        tmpDir = new File("tmp");
        tmpDir.mkdir();
    }
    
    @AfterClass
    public static void removeTmpDir() {
        FileUtils.deleteQuietly(tmpDir);
    }
    
    @Before
    public void setup() throws Exception {
        
        fileName = getClass().getResource("/saved.tar").getFile();
        
        in = new TarArchiveInputStream(new FileInputStream(fileName));
        TarArchiveEntry entry;
        while ((entry = in.getNextTarEntry()) != null) {
            entryName = entry.getName();
            if ("saved/profile.xml".equals(entryName)) {
                size = entry.getSize();
                modTime = entry.getModTime();
                metaData = new RequestMetaData(modTime.getTime(), size, "profile.xml");
                identifier = new RequestIdentifier(ArchiveFileUtils.toTarUri(new File(fileName).toURI(), entryName));
                break;
            }
        }
        tarResource = new TarEntryIdentificationRequest(metaData, identifier, 3, 5, tmpDir);
        tarResource.open(in);
    }
    
    @After
    public void tearDown() throws IOException {
        in.close();
        tarResource.close();
    }
    
    @Test
    public void testTwoArgContructor() throws IOException {
        tarResource = new TarEntryIdentificationRequest(metaData, identifier, tmpDir);
        tarResource.open(in);
        //assertEquals(1, tarResource.getCache().getBuffers().size());
        assertNull(tarResource.getCache().getSourceFile());
    }
    
    @Test
    public void testGetSize() {
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><profiles/>";
        byte[] expectedBytes = expected.getBytes();
        assertEquals(size, tarResource.size());
        assertEquals(expectedBytes.length, tarResource.size());
    }
    
    @Test
    public void testGetEveryByteSequencially() {
        
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><profiles/>";
        int resourceSize = (int) tarResource.size();
        byte[] bin = new byte[resourceSize];
        
        int i;
        for (i = 0; i < size; i++) {
            bin[i] = tarResource.getByte(i);
        }
        //assertEquals(3, tarResource.getCache().getBuffers().size());
        
        assertEquals(expected, new String(bin));
        
        try {
            tarResource.getByte(i);
            //fail("Expected IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
            //assertEquals("No byte at position [" + i + "]", e.getMessage());
        }
        
    }
    
    @Test
    public void testGetByte3FollowedByByte42() {
        
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><profiles/>";
        assertEquals(expected.getBytes()[3], tarResource.getByte(3));
        assertEquals(expected.getBytes()[42], tarResource.getByte(42));
        //assertEquals(2, tarResource.getCache().getBuffers().size());
        
    }

    @Test
    public void testGetByte42FollowedByByte3() {
        
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><profiles/>";
        assertEquals(expected.getBytes()[42], tarResource.getByte(42));
        assertEquals(expected.getBytes()[3], tarResource.getByte(3));
        //assertEquals(2, tarResource.getCache().getBuffers().size());
        
    }

    @Test
    public void testGetByte42FollowedByByte49() {
        
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><profiles/>";
        assertEquals(expected.getBytes()[42], tarResource.getByte(42));
        try {
            tarResource.getByte(49);
            //fail("Expected IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
            //assertEquals("No byte at position [49]", e.getMessage());
        }
        //assertEquals(3, tarResource.getCache().getBuffers().size());
        
    }

    @Test
    public void testGetByte42FollowedByByte10000() {
        
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><profiles/>";
        assertEquals(expected.getBytes()[42], tarResource.getByte(42));
        try {
            tarResource.getByte(10000);
            //fail("Expected IndexOutOfBoundsException.");
        } catch (IndexOutOfBoundsException e) {
            //assertEquals("No byte at position [10000]", e.getMessage());
        }
        
    }
    
    @Test
    public void testGetExtension() {
        assertEquals(identifier, tarResource.getIdentifier());
        
        assertEquals("xml", tarResource.getExtension());
        assertEquals("profile.xml", tarResource.getFileName());
        assertEquals("tar:" + new File(fileName).toURI() + "!/saved/profile.xml",
                tarResource.getIdentifier().getUri().toString());
        assertEquals(metaData, tarResource.getRequestMetaData());
        
    }
    
    @Test
    public void testCloseDeletesTheBinaryCacheFile() throws IOException {
        assertTrue(tarResource.getTempFile().exists());
        tarResource.close();
        //assertFalse(tarResource.getTempFile().exists());
        assertTrue(tarResource.getTempFile() == null);
    }

}
