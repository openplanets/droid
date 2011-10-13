/**
 * <p>Copyright (c) The National Archives 2005-2010.  All rights reserved.
 * See Licence.txt for full licence details.
 * <p/>
 *
 * <p>DROID DCS Profile Tool
 * <p/>
 */
package uk.gov.nationalarchives.droid.submitter;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import uk.gov.nationalarchives.droid.core.interfaces.ResourceId;
import uk.gov.nationalarchives.droid.submitter.FileWalker.ProgressEntry;

/**
 * @author rflitcroft
 *
 */
public class FileWalkerTest {

    private static File[] files = new File[30];
    private static final File TEST_ROOT = new File("tmpFileWalker");
    
    @Before
    public void setup() throws Exception {

        
        String[] fileNames = new String[] { 
            "dir1/file11.ext",
            "dir1/file12.ext",
            "dir1/file13.ext", 
            "dir1/file14.ext", 
            "dir1/file15.ext",

            "dir2/file21.ext", 
            "dir2/file22.ext", 
            "dir2/file23.ext",
            "dir2/file24.ext", 
            "dir2/file25.ext",

            "dir1/subdir1/file111.ext", 
            "dir1/subdir1/file112.ext",
            "dir1/subdir1/file113.ext", 
            "dir1/subdir1/file114.ext",
            "dir1/subdir1/file115.ext",

            "dir1/subdir2/file121.ext", 
            "dir1/subdir2/file122.ext",
            "dir1/subdir2/file123.ext", 
            "dir1/subdir2/file124.ext",
            "dir1/subdir2/file125.ext",

            "dir2/subdir1/file211.ext", 
            "dir2/subdir1/file212.ext",
            "dir2/subdir1/file213.ext", 
            "dir2/subdir1/file214.ext",
            "dir2/subdir1/file215.ext",

            "dir2/subdir2/file221.ext", 
            "dir2/subdir2/file222.ext",
            "dir2/subdir2/file223.ext", 
            "dir2/subdir2/file224.ext",
            "dir2/subdir2/file225.ext", 
        };

        TEST_ROOT.mkdir();

        
        for (int i = 0; i < fileNames.length; i++) {
            files[i] = new File(TEST_ROOT, fileNames[i]).getAbsoluteFile();
            files[i].getParentFile().mkdirs();
            files[i].createNewFile();
        }
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.forceDelete(TEST_ROOT);
    }
    
    /**
     * WARNING: platform specific behaviour in this test.   On windows and unix, the files
     * are walked in a reverse order to each other.  For example, the verification of 7 times 
     * on Windows comes in as 23 times on unix (and vice versa), as there are 30 files in total.
     * 
     * @throws Exception
     */
    @Test
    public void testFastForwardFromUnprocessedFile() throws Exception {
        
        final AtomicLong nextId = new AtomicLong(0); 
        FileWalker fileWalker = new FileWalker(TEST_ROOT.toURI(), true);
        
        FileWalkerHandler directoryHandler = mock(FileWalkerHandler.class);
        FileWalkerHandler fileHandler = mock(FileWalkerHandler.class);
        
        fileWalker.setDirectoryHandler(directoryHandler);
        fileWalker.setFileHandler(fileHandler);
        
        when(fileHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenAnswer(new Answer<ResourceId>() {
                @Override
                public ResourceId answer(InvocationOnMock invocation) throws Throwable {
                    File breakFile = files[23];
                    File thisFile = (File) invocation.getArguments()[0];
                    int depth = (Integer) invocation.getArguments()[1];
                    if (thisFile.equals(breakFile)) {
                        throw new DirectoryWalker.CancelException(thisFile, depth);
                    }
                    return new ResourceId(nextId.incrementAndGet(), "");
                } 
            });
        
        when(directoryHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenAnswer(new Answer<ResourceId>() {
                @Override
                public ResourceId answer(InvocationOnMock invocation) throws Throwable {
                    return new ResourceId(nextId.incrementAndGet(), "");
                } 
            });

        
        try {
            fileWalker.walk();
            fail("Expected file walker to throw exception");
        } catch (DirectoryWalker.CancelException e) {
            assertEquals(files[23], e.getFile());
        }
        
        FileWalkerHandler resumeHandler = mock(FileWalkerHandler.class);
        when(resumeHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenReturn(new ResourceId(nextId.incrementAndGet(), ""));
        fileWalker.setFileHandler(resumeHandler);
        
        fileWalker.walk();
        
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(resumeHandler, times(7)).handle(fileCaptor.capture(), anyInt(), any(ProgressEntry.class));

        File[] resumedFiles = fileCaptor.getAllValues().toArray(new File[0]);
        for (int i = 0; i < resumedFiles.length; i++) {
            assertEquals(files[23 + i], resumedFiles[i]);
        }
        
    }

    /**
     * WARNING: platform specific behaviour in this test.   On windows and unix, the files
     * are walked in a reverse order to each other.  This test was written and works on Windows.
     * 
     * @throws Exception
     */
    @Test
    public void testFastForwardFromMissingUnprocessedFile() throws Exception {
        
        final AtomicLong nextId = new AtomicLong(0); 
        FileWalker fileWalker = new FileWalker(TEST_ROOT.toURI(), true);
        
        FileWalkerHandler directoryHandler = mock(FileWalkerHandler.class);
        FileWalkerHandler fileHandler = mock(FileWalkerHandler.class);
        
        fileWalker.setDirectoryHandler(directoryHandler);
        fileWalker.setFileHandler(fileHandler);
        
        when(fileHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenAnswer(new Answer<ResourceId>() {
                @Override
                public ResourceId answer(InvocationOnMock invocation) throws Throwable {
                    File breakFile = files[23];
                    File thisFile = (File) invocation.getArguments()[0];
                    int depth = (Integer) invocation.getArguments()[1];
                    if (thisFile.equals(breakFile)) {
                        throw new DirectoryWalker.CancelException(thisFile, depth);
                    }
                    return new ResourceId(nextId.incrementAndGet(), "");
                } 
            });
        when(directoryHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenAnswer(new Answer<ResourceId>() {
                @Override
                public ResourceId answer(InvocationOnMock invocation) throws Throwable {
                    return new ResourceId(nextId.incrementAndGet(), "");
                } 
            });
        
        
        try {
            fileWalker.walk();
            fail("Expected file walker to throw exception");
        } catch (DirectoryWalker.CancelException e) {
            assertEquals(files[23], e.getFile());
        }
        
        files[23].delete();
        
        FileWalkerHandler resumeHandler = mock(FileWalkerHandler.class);
        when(resumeHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenReturn(new ResourceId(nextId.incrementAndGet(), ""));
        fileWalker.setFileHandler(resumeHandler);
        
        fileWalker.walk();
        
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(resumeHandler, times(6)).handle(fileCaptor.capture(), anyInt(), any(ProgressEntry.class));

        File[] resumedFiles = fileCaptor.getAllValues().toArray(new File[0]);
        for (int i = 0; i < resumedFiles.length; i++) {
            assertEquals(files[24 + i], resumedFiles[i]);
        }
        
    }

    /**
     * WARNING: platform specific behaviour in this test.   On windows and unix, the files
     * are walked in a reverse order to each other.  This test was written and works on Windows.
     * 
     * @throws Exception
     */
    @Test
    public void testFastForwardFromMissingButPartiallyProcessedDirectory() throws Exception {
        
        final AtomicLong nextId = new AtomicLong(0); 
        FileWalker fileWalker = new FileWalker(TEST_ROOT.toURI(), true);
        
        FileWalkerHandler directoryHandler = mock(FileWalkerHandler.class);
        FileWalkerHandler fileHandler = mock(FileWalkerHandler.class);
        
        fileWalker.setDirectoryHandler(directoryHandler);
        fileWalker.setFileHandler(fileHandler);
        
        when(fileHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenAnswer(new Answer<ResourceId>() {
                @Override
                public ResourceId answer(InvocationOnMock invocation) throws Throwable {
                    File breakFile = files[23];
                    File thisFile = (File) invocation.getArguments()[0];
                    int depth = (Integer) invocation.getArguments()[1];
                    if (thisFile.equals(breakFile)) {
                        throw new DirectoryWalker.CancelException(thisFile, depth);
                    }
                    return new ResourceId(nextId.incrementAndGet(), "");
                } 
            });
        when(directoryHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenAnswer(new Answer<ResourceId>() {
                @Override
                public ResourceId answer(InvocationOnMock invocation) throws Throwable {
                    return new ResourceId(nextId.incrementAndGet(), "");
                } 
            });
        
        
        try {
            fileWalker.walk();
            fail("Expected file walker to throw exception");
        } catch (DirectoryWalker.CancelException e) {
            assertEquals(files[23], e.getFile());
        }
        
        FileUtils.deleteDirectory(files[23].getParentFile());
        
        FileWalkerHandler resumeHandler = mock(FileWalkerHandler.class);
        when(resumeHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenReturn(new ResourceId(nextId.incrementAndGet(), ""));
        fileWalker.setFileHandler(resumeHandler);
        
        fileWalker.walk();
        
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(resumeHandler, times(5)).handle(fileCaptor.capture(), anyInt(), any(ProgressEntry.class));

        File[] resumedFiles = fileCaptor.getAllValues().toArray(new File[0]);
        for (int i = 0; i < resumedFiles.length; i++) {
            assertEquals(files[25 + i], resumedFiles[i]);
        }
        
    }

    /**
     * WARNING: platform specific behaviour in this test.   On windows and unix, the files
     * are walked in a reverse order to each other.  This test was written and works on Windows.
     * 
     * @throws Exception
     */
    @Test
    public void testFastForwardFromEmptyButPartiallyProcessedDirectory() throws Exception {
        
        final AtomicLong nextId = new AtomicLong(0); 
        FileWalker fileWalker = new FileWalker(TEST_ROOT.toURI(), true);
        
        FileWalkerHandler directoryHandler = mock(FileWalkerHandler.class);
        FileWalkerHandler fileHandler = mock(FileWalkerHandler.class);
        
        fileWalker.setDirectoryHandler(directoryHandler);
        fileWalker.setFileHandler(fileHandler);
        
        when(fileHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenAnswer(new Answer<ResourceId>() {
                @Override
                public ResourceId answer(InvocationOnMock invocation) throws Throwable {
                    File breakFile = files[23];
                    File thisFile = (File) invocation.getArguments()[0];
                    int depth = (Integer) invocation.getArguments()[1];
                    if (thisFile.equals(breakFile)) {
                        throw new DirectoryWalker.CancelException(thisFile, depth);
                    }
                    return new ResourceId(nextId.incrementAndGet(), "");
                } 
            });
        when(directoryHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenAnswer(new Answer<ResourceId>() {
                @Override
                public ResourceId answer(InvocationOnMock invocation) throws Throwable {
                    return new ResourceId(nextId.incrementAndGet(), "");
                } 
            });
        
        
        try {
            fileWalker.walk();
            fail("Expected file walker to throw exception");
        } catch (DirectoryWalker.CancelException e) {
            assertEquals(files[23], e.getFile());
        }
        
        final File directoryToEmpty = files[23].getParentFile();
        for (File f : directoryToEmpty.listFiles()) {
            f.delete();
        }
        assertEquals(0, directoryToEmpty.listFiles().length);
        
        FileWalkerHandler resumeHandler = mock(FileWalkerHandler.class);
        when(resumeHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenReturn(new ResourceId(nextId.incrementAndGet(), ""));
        fileWalker.setFileHandler(resumeHandler);
        
        fileWalker.walk();
        
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(resumeHandler, times(5)).handle(fileCaptor.capture(), anyInt(), any(ProgressEntry.class));

        File[] resumedFiles = fileCaptor.getAllValues().toArray(new File[0]);
        for (int i = 0; i < resumedFiles.length; i++) {
            assertEquals(files[25 + i], resumedFiles[i]);
        }
        
    }

    /**
     * WARNING: platform specific behaviour in this test.   On windows and unix, the files
     * are walked in a reverse order to each other.  This test was written and works on Windows.
     * 
     * @throws Exception
     */
    @Test
    public void testFastForwardFromPartiallyProcessedDirectoryWithANewFile() throws Exception {
        
        final AtomicLong nextId = new AtomicLong(0); 
        FileWalker fileWalker = new FileWalker(TEST_ROOT.toURI(), true);
        
        FileWalkerHandler directoryHandler = mock(FileWalkerHandler.class);
        FileWalkerHandler fileHandler = mock(FileWalkerHandler.class);
        
        fileWalker.setDirectoryHandler(directoryHandler);
        fileWalker.setFileHandler(fileHandler);
        
        when(fileHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenAnswer(new Answer<ResourceId>() {
                @Override
                public ResourceId answer(InvocationOnMock invocation) throws Throwable {
                    File breakFile = files[23];
                    File thisFile = (File) invocation.getArguments()[0];
                    int depth = (Integer) invocation.getArguments()[1];
                    if (thisFile.equals(breakFile)) {
                        throw new DirectoryWalker.CancelException(thisFile, depth);
                    }
                    return new ResourceId(nextId.incrementAndGet(), "");
                } 
            });
        when(directoryHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenAnswer(new Answer<ResourceId>() {
                @Override
                public ResourceId answer(InvocationOnMock invocation) throws Throwable {
                    return new ResourceId(nextId.incrementAndGet(), "");
                } 
            });
        
        
        try {
            fileWalker.walk();
            fail("Expected file walker to throw exception");
        } catch (DirectoryWalker.CancelException e) {
            assertEquals(files[23], e.getFile());
        }
        
        final File newFile = new File(files[21].getPath() + "a");
        assertTrue(newFile.createNewFile());

        FileWalkerHandler resumeHandler = mock(FileWalkerHandler.class);
        when(resumeHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenReturn(new ResourceId(nextId.incrementAndGet(), ""));
        fileWalker.setFileHandler(resumeHandler);
        
        fileWalker.walk();
        
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(resumeHandler, times(7)).handle(fileCaptor.capture(), anyInt(), any(ProgressEntry.class));

        File[] resumedFiles = fileCaptor.getAllValues().toArray(new File[0]);
        for (int i = 0; i < resumedFiles.length; i++) {
            assertEquals(files[23 + i], resumedFiles[i]);
        }
        
    }

    /**
     * WARNING: platform specific behaviour in this test.   On windows and unix, the files
     * are walked in a reverse order to each other.  This test was written and works on Windows.
     * 
     * @throws Exception
     */
    @Test
    public void testFastForwardFromPartiallyProcessedDirectoryWithANewDirectory() throws Exception {
        
        final AtomicLong nextId = new AtomicLong(0); 
        FileWalker fileWalker = new FileWalker(TEST_ROOT.toURI(), true);
        
        FileWalkerHandler directoryHandler = mock(FileWalkerHandler.class);
        FileWalkerHandler fileHandler = mock(FileWalkerHandler.class);
        
        fileWalker.setDirectoryHandler(directoryHandler);
        fileWalker.setFileHandler(fileHandler);
        
        when(fileHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenAnswer(new Answer<ResourceId>() {
                @Override
                public ResourceId answer(InvocationOnMock invocation) throws Throwable {
                    File breakFile = files[23];
                    File thisFile = (File) invocation.getArguments()[0];
                    int depth = (Integer) invocation.getArguments()[1];
                    if (thisFile.equals(breakFile)) {
                        throw new DirectoryWalker.CancelException(thisFile, depth);
                    }
                    return new ResourceId(nextId.incrementAndGet(), "");
                } 
            });
        when(directoryHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenAnswer(new Answer<ResourceId>() {
                @Override
                public ResourceId answer(InvocationOnMock invocation) throws Throwable {
                    return new ResourceId(nextId.incrementAndGet(), "");
                } 
            });
        
        
        try {
            fileWalker.walk();
            fail("Expected file walker to throw exception");
        } catch (DirectoryWalker.CancelException e) {
            assertEquals(files[23], e.getFile());
        }
        
        final File newFile = new File(files[21].getPath() + "a");
        assertTrue(newFile.mkdir());

        FileWalkerHandler resumeHandler = mock(FileWalkerHandler.class);
        when(resumeHandler.handle(any(File.class), anyInt(), any(ProgressEntry.class)))
            .thenReturn(new ResourceId(nextId.incrementAndGet(), ""));
        fileWalker.setFileHandler(resumeHandler);
        
        fileWalker.walk();
        
        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(resumeHandler, times(7)).handle(fileCaptor.capture(), anyInt(), any(ProgressEntry.class));

        File[] resumedFiles = fileCaptor.getAllValues().toArray(new File[0]);
        for (int i = 0; i < resumedFiles.length; i++) {
            assertEquals(files[23 + i], resumedFiles[i]);
        }
        
    }
}
