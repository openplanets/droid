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
import java.io.IOException;
import java.io.InputStream;

import net.domesdaybook.reader.ByteReader;

import uk.gov.nationalarchives.droid.core.interfaces.IdentificationRequest;
import uk.gov.nationalarchives.droid.core.interfaces.RequestIdentifier;

/**
 * Encapsulated the binary data for a file system identification request.
 * @author rflitcroft
 *
 */
public class FileSystemIdentificationRequest implements IdentificationRequest {

    private static final int BUFFER_CACHE_CAPACITY = 16;
    private static final int CAPACITY = 32 * 1024; 

    private final String extension;
    private final String fileName;
    private final long size;

    private CachedBytes cachedBinary;
    private final RequestIdentifier identifier;

    private final int lruCapacity;
    private final int bufferCapacity;
    
    private RequestMetaData requestMetaData;

    /**
     * Constructs a new identification request.
     * @param metaData the metaData about the binary.
     * @param lruCapacity the buffer cache capacity
     * @param identifier the request's identifier
     * @param bufferCapacity the buffer capacity
     */
    FileSystemIdentificationRequest(RequestMetaData metaData, RequestIdentifier identifier,
            int lruCapacity, int bufferCapacity) {
        this.identifier = identifier;
        
        requestMetaData = metaData;
        size = metaData.getSize();
        fileName = metaData.getName();
        //extension = FilenameUtils.getExtension(fileName);
        extension = ResourceUtils.getExtension(fileName);
        this.lruCapacity = lruCapacity;
        this.bufferCapacity = bufferCapacity;

    }
    
    /**
     * Constructs a new identification request.
     * @param metaData the metaData about the binary.
     * @param identifier the request's identifier
     */
    public FileSystemIdentificationRequest(RequestMetaData metaData, RequestIdentifier identifier) {
        this(metaData, identifier, BUFFER_CACHE_CAPACITY, CAPACITY);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final void open(InputStream in) throws IOException {
        /* using normal stream access and CachedByteArrays */
        byte[] firstBuffer = new byte[bufferCapacity];
        int bytesRead = ResourceUtils.readBuffer(in, firstBuffer);
        if (bytesRead < 1) {
            firstBuffer = new byte[0];
            cachedBinary = new CachedByteArray(firstBuffer, 0);
        } else if (bytesRead < bufferCapacity) {
            // size the buffer to the amount of bytes available:
            // firstBuffer = Arrays.copyOf(firstBuffer, bytesRead);
            cachedBinary = new CachedByteArray(firstBuffer, bytesRead);
        } else {
            cachedBinary = new CachedByteArrays(lruCapacity, bufferCapacity, firstBuffer, bufferCapacity);
        }
        
        /* using nio and CachedByteBuffers
        FileChannel channel = (FileChannel) Channels.newChannel(in);
        
        ByteBuffer blockZero = ByteBuffer.allocate(bufferCapacity);

        int bytesRead = 0;
        do {
            bytesRead = channel.read(blockZero);
        } while (bytesRead >= 0 && blockZero.hasRemaining());
        
        cachedBinary = new CachedByteBuffers(lruCapacity, bufferCapacity, blockZero);
        */
        
        /** Only sets source file if it is bigger than the cache.
        if (blockZero.limit() == blockZero.capacity()) {
            cachedBinary.setSourceFile(new File(identifier.getUri()));
        }
        */
        
        // Always set source file, so we can access it for file-oriented processing
        // if necessary (e.g. container identification of zip files, if the file
        // proves to be a zip file).
        final File theFile = new File(identifier.getUri());
        cachedBinary.setSourceFile(theFile);
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final byte getByte(long position) {
        return cachedBinary.readByte(position);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getExtension() {
        return extension;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final String getFileName() {
        return fileName;
    }
    
   
    /**
     * {@inheritDoc}
     */
    @Override
    public final long size() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void close() throws IOException {
        cachedBinary.close();
    }

    /**
     * @return the internal binary cache
     */
    final CachedBytes getCache() {
        return cachedBinary;
    }
    
    /**
     * {@inheritDoc}
     * @throws IOException 
     */
    @Override
    public final InputStream getSourceInputStream() throws IOException {
        return cachedBinary.getSourceInputStream();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final RequestMetaData getRequestMetaData() {
        return requestMetaData;
    }

    /**
     * @return the identifier
     */
    public final RequestIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ByteReader getReader() {
        return cachedBinary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final File getSourceFile() throws IOException {
        // File system identification requests always set the source file.
        return cachedBinary.getSourceFile();
    }

}
